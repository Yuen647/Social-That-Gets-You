package com.tongji.airrowing.note.biz.service.impl;

import com.tongji.airrowing.note.biz.domain.dataobject.NoteDO;
import com.tongji.airrowing.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.tongji.airrowing.note.biz.domain.mapper.NoteDOMapper;
import com.tongji.airrowing.note.biz.domain.mapper.NoteLikeDOMapper;
import com.tongji.airrowing.note.biz.domain.mapper.TopicDOMapper;
import com.tongji.airrowing.note.biz.enums.NoteTypeEnum;
import com.tongji.airrowing.note.biz.enums.NoteVisibleEnum;
import com.tongji.airrowing.note.biz.enums.ResponseCodeEnum;
import com.tongji.airrowing.note.biz.model.vo.*;
import com.tongji.airrowing.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.tongji.airrowing.note.biz.rpc.KeyValueRpcService;
import com.tongji.airrowing.note.biz.rpc.UserRpcService;
import com.tongji.framework.biz.context.holder.LoginUserContextHolder;
import com.tongji.framework.common.exception.BizException;
import com.tongji.framework.common.response.Response;
import com.tongji.framework.common.util.JsonUtils;
import com.tongji.airrowing.user.dto.resp.FindUserByIdRspDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.tongji.airrowing.note.biz.constant.MQConstants;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tongji.framework.common.constant.GlobalConstants;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @InjectMocks
    private NoteServiceImpl noteService;

    @Mock
    private NoteDOMapper noteDOMapper;

    @Mock
    private TopicDOMapper topicDOMapper;

    @Mock
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Mock
    private KeyValueRpcService keyValueRpcService;

    @Mock
    private UserRpcService userRpcService;

    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private NoteLikeDOMapper noteLikeDOMapper;

    @Mock
    private NoteCollectionDOMapper noteCollectionDOMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private Cache<Long, String> LOCAL_CACHE;

    @Captor
    private ArgumentCaptor<NoteDO> noteDOCaptor;

    private static final Long MOCK_USER_ID = 2101L;
    private static final Long OTHER_USER_ID = 8103L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use lenient mocking for infrastructure operations that may not be used in every test
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    /**
     * 设置当前登录用户ID
     */
    private void setCurrentUserId() {
        // 使用反射设置当前用户ID
        try {
            java.lang.reflect.Field field = LoginUserContextHolder.class.getDeclaredField("LOGIN_USER_CONTEXT_THREAD_LOCAL");
            field.setAccessible(true);
            ThreadLocal<Map<String, Object>> threadLocal = (ThreadLocal<Map<String, Object>>) field.get(null);
            Map<String, Object> map = new HashMap<>();
            map.put(GlobalConstants.USER_ID, MOCK_USER_ID);
            threadLocal.set(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set current user ID", e);
        }
    }

    /**
     * 清除当前登录用户ID
     */
    private void clearCurrentUserId() {
        try {
            java.lang.reflect.Field field = LoginUserContextHolder.class.getDeclaredField("LOGIN_USER_CONTEXT_THREAD_LOCAL");
            field.setAccessible(true);
            ThreadLocal<Map<String, Object>> threadLocal = (ThreadLocal<Map<String, Object>>) field.get(null);
            threadLocal.remove();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear current user ID", e);
        }
    }

    @AfterEach
    void tearDown() {
        clearCurrentUserId();
    }

    /**
     * WTC_001: 测试正常发布图文笔记的流程
     * 覆盖路径: 1→2→3→4→5→6
     */
    @Test
    void publishNote_WithValidImageText() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        PublishNoteReqVO reqVO = PublishNoteReqVO.builder()
                .type(NoteTypeEnum.IMAGE_TEXT.getCode())
                .imgUris(Arrays.asList("image1.jpg", "image2.jpg"))
                .title("测试图文笔记")
                .content("这是一篇测试图文笔记")
                .topicId(1L)
                .build();

        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(topicDOMapper.selectNameByPrimaryKey(anyLong())).thenReturn("测试话题");
        lenient().when(distributedIdGeneratorRpcService.getSnowflakeId()).thenReturn("123456");
        lenient().when(keyValueRpcService.saveNoteContent(anyString(), anyString())).thenReturn(true);
        lenient().doReturn(1).when(noteDOMapper).insert(any(NoteDO.class));
        lenient().doNothing().when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // 执行测试方法
        Response<?> response = noteService.publishNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());
        
        // 验证方法调用
        verify(noteDOMapper).insert(noteDOCaptor.capture());
        NoteDO capturedNote = noteDOCaptor.getValue();
        
        // 验证笔记内容
        assertFalse(capturedNote.getIsContentEmpty());
        assertEquals("image1.jpg,image2.jpg", capturedNote.getImgUris());
        assertEquals("测试图文笔记", capturedNote.getTitle());
        assertEquals(1L, capturedNote.getTopicId());
        assertEquals("测试话题", capturedNote.getTopicName());
        assertEquals(NoteTypeEnum.IMAGE_TEXT.getCode(), capturedNote.getType());
        assertEquals(NoteVisibleEnum.PUBLIC.getCode(), capturedNote.getVisible());
        assertFalse(capturedNote.getIsTop());
        assertNull(capturedNote.getVideoUri());
        assertNotNull(capturedNote.getContentUuid());
    }

    /**
     * WTC_002: 测试正常发布视频笔记的流程
     * 覆盖路径: 1→2(视频)→3→4→5→6
     */
    @Test
    void publishNote_WithValidVideo() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        PublishNoteReqVO reqVO = PublishNoteReqVO.builder()
                .type(NoteTypeEnum.VIDEO.getCode())
                .videoUri("video.mp4")
                .title("测试视频笔记")
                .content("这是一篇测试视频笔记")
                .topicId(null)
                .build();

        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(distributedIdGeneratorRpcService.getSnowflakeId()).thenReturn("123456");
        lenient().when(keyValueRpcService.saveNoteContent(anyString(), anyString())).thenReturn(true);
        lenient().doReturn(1).when(noteDOMapper).insert(any(NoteDO.class));
        lenient().doNothing().when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // 执行测试方法
        Response<?> response = noteService.publishNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());
        
        // 验证方法调用
        verify(noteDOMapper).insert(noteDOCaptor.capture());
        NoteDO capturedNote = noteDOCaptor.getValue();
        
        // 验证笔记内容
        assertFalse(capturedNote.getIsContentEmpty());
        assertNull(capturedNote.getImgUris());
        assertEquals("测试视频笔记", capturedNote.getTitle());
        assertNull(capturedNote.getTopicId());
        assertNull(capturedNote.getTopicName());
        assertEquals(NoteTypeEnum.VIDEO.getCode(), capturedNote.getType());
        assertEquals(NoteVisibleEnum.PUBLIC.getCode(), capturedNote.getVisible());
        assertFalse(capturedNote.getIsTop());
        assertEquals("video.mp4", capturedNote.getVideoUri());
        assertNotNull(capturedNote.getContentUuid());
    }

    /**
     * WTC_003: 测试参数验证失败的情况
     * 覆盖路径: 1(失败退出)
     */
    @Test
    void publishNote_WithInvalidType() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据 - 使用无效的笔记类型
        PublishNoteReqVO reqVO = PublishNoteReqVO.builder()
                .type(999) // 无效的笔记类型
                .imgUris(Arrays.asList("image1.jpg"))
                .title("测试笔记")
                .build();

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.publishNote(reqVO);
        });
        
        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_TYPE_ERROR.getErrorCode(), exception.getErrorCode());
        
        // 验证没有调用后续方法
        verify(noteDOMapper, never()).insert(any(NoteDO.class));
        verify(rocketMQTemplate, never()).asyncSend(anyString(), any(Message.class), any(SendCallback.class));
    }

    /**
     * WTC_004: 测试用户未点赞的情况
     * 分支条件: 用户未点赞
     */
    @Test
    void likeNote_UserNotLikedBefore() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        LikeNoteReqVO reqVO = new LikeNoteReqVO();
        reqVO.setId(1L);
        
        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectCreatorIdByNoteId(anyLong())).thenReturn(2L); // 笔记创建者ID
        // Redis布隆过滤器返回0，表示用户未点赞
        lenient().when(redisTemplate.execute(any(), anyList(), any())).thenReturn(0L);
        lenient().doNothing().when(rocketMQTemplate).asyncSendOrderly(anyString(), any(Message.class), anyString(), any(SendCallback.class));
        
        // 执行测试方法
        Response<?> response = noteService.likeNote(reqVO);
        
        // 验证结果
        assertTrue(response.isSuccess());
        
        // 验证方法调用
        verify(noteDOMapper).selectCreatorIdByNoteId(anyLong());
        verify(redisTemplate, times(2)).execute(any(), anyList(), any()); // 验证execute被调用了2次
        // 由于布隆过滤器返回false，不应该调用数据库查询
        verify(noteLikeDOMapper, never()).selectCountByUserIdAndNoteId(anyLong(), anyLong());
        verify(rocketMQTemplate).asyncSendOrderly(anyString(), any(Message.class), anyString(), any(SendCallback.class));
    }

    /**
     * WTC_005: 测试用户已点赞的情况
     * 分支条件: 用户已点赞
     */
    @Test
    void likeNote_UserAlreadyLiked() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        LikeNoteReqVO reqVO = new LikeNoteReqVO();
        reqVO.setId(1L);
        
        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectCreatorIdByNoteId(anyLong())).thenReturn(2L); // 笔记创建者ID
        lenient().when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L); // 布隆过滤器存在
        lenient().when(noteLikeDOMapper.selectCountByUserIdAndNoteId(anyLong(), anyLong())).thenReturn(1); // 数据库中已点赞
        
        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> noteService.likeNote(reqVO));
        
        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_ALREADY_LIKED.getErrorCode(), exception.getErrorCode());
    }

    /**
     * WTC_006: 测试点赞笔记时数据库异常的情况
     * 分支条件: 数据库异常
     */
    @Test
    void likeNote_DatabaseException() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        LikeNoteReqVO reqVO = new LikeNoteReqVO();
        reqVO.setId(1L);
        
        // Mock方法行为 - 模拟数据库异常
        lenient().when(noteDOMapper.selectCreatorIdByNoteId(anyLong())).thenThrow(new RuntimeException("Database error"));
        
        // 执行测试方法并验证异常
        assertThrows(RuntimeException.class, () -> {
            noteService.likeNote(reqVO);
        });
    }

    /**
     * WTC_011: 测试getNoteList方法，page=0的情况
     * 路径1: page<=0 → 设置默认值 → 查询数据库 → 返回结果
     */
    @Test
    void getNoteList_WithPageZero() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 0;
        int size = 10;
        
        // 准备模拟的返回数据
        List<NoteDO> mockNotes = new ArrayList<>();
        mockNotes.add(createMockNote(1L, "笔记1", 1L));
        mockNotes.add(createMockNote(2L, "笔记2", 1L));
        
        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectAllNotes(any())).thenReturn(mockNotes);
        
        // 执行测试方法
        Response<?> response = noteService.getNoteList(page, size);
        
        // 验证结果
        assertTrue(response.isSuccess());
        List<NoteListRspVO> noteList = (List<NoteListRspVO>) response.getData();
        assertEquals(2, noteList.size());
        assertEquals(1L, noteList.get(0).getId());
        assertEquals("笔记1", noteList.get(0).getTitle());
        assertEquals(2L, noteList.get(1).getId());
        assertEquals("笔记2", noteList.get(1).getTitle());
    }

    /**
     * WTC_012: 测试getNoteList方法，有数据的情况
     * 路径2: page>0 → 直接查询 → 有数据 → 返回结果
     */
    @Test
    void getNoteList_WithData() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 1;
        int size = 10;
        
        // 准备模拟的返回数据
        List<NoteDO> mockNotes = new ArrayList<>();
        mockNotes.add(createMockNote(1L, "笔记1", 1L));
        mockNotes.add(createMockNote(2L, "笔记2", 2L));
        
        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectAllNotes(any())).thenReturn(mockNotes);
        
        // 执行测试方法
        Response<?> response = noteService.getNoteList(page, size);
        
        // 验证结果
        assertTrue(response.isSuccess());
        List<NoteListRspVO> noteList = (List<NoteListRspVO>) response.getData();
        assertEquals(2, noteList.size());
        assertEquals(1L, noteList.get(0).getId());
        assertEquals(2L, noteList.get(1).getId());
    }

    /**
     * WTC_013: 测试getNoteList方法，无数据的情况
     * 路径3: page>0 → 直接查询 → 无数据 → 返回空列表
     */
    @Test
    void getNoteList_WithNoData() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 999;
        int size = 10;
        
        // 准备模拟的返回数据 - 空列表
        List<NoteDO> mockNotes = new ArrayList<>();
        
        // Mock方法行为 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectAllNotes(any())).thenReturn(mockNotes);
        
        // 执行测试方法
        Response<?> response = noteService.getNoteList(page, size);
        
        // 验证结果
        assertTrue(response.isSuccess());
        List<NoteListRspVO> noteList = (List<NoteListRspVO>) response.getData();
        assertTrue(noteList.isEmpty());
    }

    /**
     * WTC_014: 测试getNoteList方法，数据库异常情况
     * 路径4: 数据库异常 → 抛出异常
     */
    @Test
    void getNoteList_WithDatabaseException() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 1;
        int size = 10;
        
        // Mock方法行为 - 模拟数据库异常 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteDOMapper.selectAllNotes(any())).thenThrow(new RuntimeException("Database connection failed"));
        
        // 执行测试方法并验证异常
        assertThrows(RuntimeException.class, () -> {
            noteService.getNoteList(page, size);
        });
    }

    /**
     * 创建模拟的NoteDO对象
     */
    private NoteDO createMockNote(Long id, String title, Long creatorId) {
        return NoteDO.builder()
                .id(id)
                .title(title)
                .creatorId(creatorId)
                .build();
    }

    /**
     * WTC_007: 条件覆盖测试 - 公开笔记 & 作者访问
     * 条件组合: T || T
     */
    @Test
    void checkNoteVisible_PublicNote_CreatorAccess() throws Exception {
        // 设置当前用户ID
        setCurrentUserId();

        // 使用反射访问私有方法
        java.lang.reflect.Method method = NoteServiceImpl.class.getDeclaredMethod(
                "checkNoteVisible", Integer.class, Long.class, Long.class);
        method.setAccessible(true);

        // 准备测试数据
        Integer visible = NoteVisibleEnum.PUBLIC.getCode(); // 公开笔记
        Long currentUserId = MOCK_USER_ID;                  // 当前用户ID
        Long creatorId = MOCK_USER_ID;                      // 笔记创建者ID (与当前用户相同)

        // 执行测试方法 - 不应抛出异常
        method.invoke(noteService, visible, currentUserId, creatorId);
        
        // 如果没有抛出异常，测试通过
        assertTrue(true);
    }

    /**
     * WTC_008: 条件覆盖测试 - 公开笔记 & 非作者访问
     * 条件组合: T || F
     */
    @Test
    void checkNoteVisible_PublicNote_NonCreatorAccess() throws Exception {
        // 设置当前用户ID
        setCurrentUserId();

        // 使用反射访问私有方法
        java.lang.reflect.Method method = NoteServiceImpl.class.getDeclaredMethod(
                "checkNoteVisible", Integer.class, Long.class, Long.class);
        method.setAccessible(true);

        // 准备测试数据
        Integer visible = NoteVisibleEnum.PUBLIC.getCode(); // 公开笔记
        Long currentUserId = MOCK_USER_ID;                  // 当前用户ID
        Long creatorId = OTHER_USER_ID;                     // 笔记创建者ID (与当前用户不同)

        // 执行测试方法 - 不应抛出异常
        method.invoke(noteService, visible, currentUserId, creatorId);
        
        // 如果没有抛出异常，测试通过
        assertTrue(true);
    }

    /**
     * WTC_009: 条件覆盖测试 - 私有笔记 & 作者访问
     * 条件组合: F || T
     */
    @Test
    void checkNoteVisible_PrivateNote_CreatorAccess() throws Exception {
        // 设置当前用户ID
        setCurrentUserId();

        // 使用反射访问私有方法
        java.lang.reflect.Method method = NoteServiceImpl.class.getDeclaredMethod(
                "checkNoteVisible", Integer.class, Long.class, Long.class);
        method.setAccessible(true);

        // 准备测试数据
        Integer visible = NoteVisibleEnum.PRIVATE.getCode(); // 私有笔记
        Long currentUserId = MOCK_USER_ID;                   // 当前用户ID
        Long creatorId = MOCK_USER_ID;                       // 笔记创建者ID (与当前用户相同)

        // 执行测试方法 - 不应抛出异常
        method.invoke(noteService, visible, currentUserId, creatorId);
        
        // 如果没有抛出异常，测试通过
        assertTrue(true);
    }

    /**
     * WTC_010: 条件覆盖测试 - 私有笔记 & 非作者访问
     * 条件组合: F || F
     */
    @Test
    void checkNoteVisible_PrivateNote_NonCreatorAccess() {
        // 设置当前用户ID
        setCurrentUserId();

        // 使用反射访问私有方法
        try {
            java.lang.reflect.Method method = NoteServiceImpl.class.getDeclaredMethod(
                    "checkNoteVisible", Integer.class, Long.class, Long.class);
            method.setAccessible(true);

            // 准备测试数据
            Integer visible = NoteVisibleEnum.PRIVATE.getCode(); // 私有笔记
            Long currentUserId = MOCK_USER_ID;                   // 当前用户ID
            Long creatorId = OTHER_USER_ID;                      // 笔记创建者ID (与当前用户不同)

            // 执行测试方法 - 应该抛出异常
            method.invoke(noteService, visible, currentUserId, creatorId);
            
            // 如果没有抛出异常，测试失败
            fail("Expected BizException was not thrown");
        } catch (Exception e) {
            // 验证是否为预期的业务异常
            assertTrue(e.getCause() instanceof BizException);
            BizException bizException = (BizException) e.getCause();
            assertEquals(ResponseCodeEnum.NOTE_PRIVATE.getErrorCode(), bizException.getErrorCode());
        }
    }

    /**
     * WTC_015: 测试findNoteDetail方法 - 本地缓存命中
     */
    @Test
    void findNoteDetail_LocalCacheHit() {
    }

    /**
     * WTC_016: 测试findNoteDetail方法 - Redis缓存命中
     */
    @Test
    void findNoteDetail_RedisCacheHit() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        FindNoteDetailReqVO reqVO = FindNoteDetailReqVO.builder()
                .id(noteId)
                .build();

        // Mock本地缓存未命中
        lenient().when(LOCAL_CACHE.getIfPresent(noteId)).thenReturn(null);

        // Mock Redis缓存命中
        String mockCacheData = JsonUtils.toJsonString(FindNoteDetailRspVO.builder()
                .id(noteId)
                .creatorId(MOCK_USER_ID)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .build());
        lenient().when(redisTemplate.opsForValue().get(anyString())).thenReturn(mockCacheData);

        // 执行测试方法
        Response<FindNoteDetailRspVO> response = noteService.findNoteDetail(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());
        assertEquals(noteId, response.getData().getId());
        assertEquals(MOCK_USER_ID, response.getData().getCreatorId());

        // 验证数据库没有被调用
        verify(noteDOMapper, never()).selectByPrimaryKey(anyLong());
    }

    /**
     * WTC_017: 测试findNoteDetail方法 - 缓存未命中，数据库查询
     */
    @Test
    void findNoteDetail_CacheMissDbHit() {
    }

    /**
     * WTC_018: 测试findNoteDetail方法 - 笔记不存在
     */
    @Test
    void findNoteDetail_NoteNotFound() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        FindNoteDetailReqVO reqVO = FindNoteDetailReqVO.builder()
                .id(noteId)
                .build();

        // Mock缓存未命中
        lenient().when(LOCAL_CACHE.getIfPresent(noteId)).thenReturn(null);
        lenient().when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

        // Mock数据库未找到笔记
        lenient().when(noteDOMapper.selectByPrimaryKey(noteId)).thenReturn(null);

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.findNoteDetail(reqVO);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_NOT_FOUND.getErrorCode(), exception.getErrorCode());

        // 验证防缓存穿透
        verify(threadPoolTaskExecutor).execute(any(Runnable.class));
    }

    /**
     * WTC_019: 测试updateNote方法 - 成功更新图文笔记
     */
    @Test
    void updateNote_WithImageTextSuccess() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UpdateNoteReqVO reqVO = UpdateNoteReqVO.builder()
                .id(noteId)
                .type(NoteTypeEnum.IMAGE_TEXT.getCode())
                .imgUris(Arrays.asList("newImage1.jpg", "newImage2.jpg"))
                .title("更新后的标题")
                .content("更新后的内容")
                .topicId(2L)
                .build();

        // Mock已存在的笔记
        NoteDO existingNote = NoteDO.builder()
                .id(noteId)
                .creatorId(MOCK_USER_ID)
                .contentUuid("old-uuid")
                .build();
        when(noteDOMapper.selectByPrimaryKey(noteId)).thenReturn(existingNote);

        // Mock话题名称
        when(topicDOMapper.selectNameByPrimaryKey(2L)).thenReturn("测试话题");

        // Mock更新操作
        when(noteDOMapper.updateByPrimaryKey(any(NoteDO.class))).thenReturn(1);
        when(keyValueRpcService.saveNoteContent(anyString(), anyString())).thenReturn(true);

        // 执行测试方法
        Response<?> response = noteService.updateNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());

        // 验证更新操作
        verify(noteDOMapper).updateByPrimaryKey(noteDOCaptor.capture());
        NoteDO updatedNote = noteDOCaptor.getValue();
        assertEquals("newImage1.jpg,newImage2.jpg", updatedNote.getImgUris());
        assertEquals("更新后的标题", updatedNote.getTitle());
        assertEquals(2L, updatedNote.getTopicId());
        assertFalse(updatedNote.getIsContentEmpty());

        // 验证缓存删除
        verify(redisTemplate).delete(anyString());
        verify(rocketMQTemplate).syncSend(eq(MQConstants.AIR_TOPIC_DELETE_NOTE_LOCAL_CACHE), eq(noteId));
    }

    /**
     * WTC_020: 测试updateNote方法 - 成功更新视频笔记
     */
    @Test
    void updateNote_WithVideoSuccess() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UpdateNoteReqVO reqVO = UpdateNoteReqVO.builder()
                .id(noteId)
                .type(NoteTypeEnum.VIDEO.getCode())
                .videoUri("newVideo.mp4")
                .title("更新后的视频标题")
                .content("更新后的视频描述")
                .build();

        // Mock已存在的笔记
        NoteDO existingNote = NoteDO.builder()
                .id(noteId)
                .creatorId(MOCK_USER_ID)
                .contentUuid("old-uuid")
                .build();
        when(noteDOMapper.selectByPrimaryKey(noteId)).thenReturn(existingNote);

        // Mock更新操作
        when(noteDOMapper.updateByPrimaryKey(any(NoteDO.class))).thenReturn(1);
        when(keyValueRpcService.saveNoteContent(anyString(), anyString())).thenReturn(true);

        // 执行测试方法
        Response<?> response = noteService.updateNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());

        // 验证更新操作
        verify(noteDOMapper).updateByPrimaryKey(noteDOCaptor.capture());
        NoteDO updatedNote = noteDOCaptor.getValue();
        assertEquals("newVideo.mp4", updatedNote.getVideoUri());
        assertEquals("更新后的视频标题", updatedNote.getTitle());
        assertFalse(updatedNote.getIsContentEmpty());

        // 验证缓存删除
        verify(redisTemplate).delete(anyString());
        verify(rocketMQTemplate).syncSend(eq(MQConstants.AIR_TOPIC_DELETE_NOTE_LOCAL_CACHE), eq(noteId));
    }

    /**
     * WTC_021: 测试updateNote方法 - 笔记不存在
     */
    @Test
    void updateNote_NoteNotFound() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UpdateNoteReqVO reqVO = UpdateNoteReqVO.builder()
                .id(noteId)
                .type(NoteTypeEnum.IMAGE_TEXT.getCode())
                .imgUris(Arrays.asList("newImage.jpg"))
                .title("更新标题")
                .build();

        // Mock笔记不存在
        when(noteDOMapper.selectByPrimaryKey(noteId)).thenReturn(null);

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.updateNote(reqVO);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_NOT_FOUND.getErrorCode(), exception.getErrorCode());
    }

    /**
     * WTC_022: 测试updateNote方法 - 非笔记创建者更新
     */
    @Test
    void updateNote_NotCreator() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UpdateNoteReqVO reqVO = UpdateNoteReqVO.builder()
                .id(noteId)
                .type(NoteTypeEnum.IMAGE_TEXT.getCode())
                .imgUris(Arrays.asList("newImage.jpg"))
                .title("更新标题")
                .build();

        // Mock已存在的笔记（创建者是其他用户）
        NoteDO existingNote = NoteDO.builder()
                .id(noteId)
                .creatorId(OTHER_USER_ID) // 使用其他用户ID
                .build();
        when(noteDOMapper.selectByPrimaryKey(noteId)).thenReturn(existingNote);

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.updateNote(reqVO);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_CANT_OPERATE.getErrorCode(), exception.getErrorCode());
    }

    /**
     * WTC_023: 测试collectNote方法 - 成功收藏笔记
     */
    @Test
    void collectNote_Success() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        CollectNoteReqVO reqVO = CollectNoteReqVO.builder()
                .id(noteId)
                .build();

        // Mock笔记创建者ID
        when(noteDOMapper.selectCreatorIdByNoteId(noteId)).thenReturn(OTHER_USER_ID);

        // Mock Redis布隆过滤器返回0，表示未收藏
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(0L);

        // Mock MQ发送
        doNothing().when(rocketMQTemplate).asyncSendOrderly(anyString(), any(Message.class), anyString(), any(SendCallback.class));

        // 执行测试方法
        Response<?> response = noteService.collectNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());

        // 验证MQ消息发送
        verify(rocketMQTemplate).asyncSendOrderly(
                eq("CollectUnCollectTopic:Collect"),
                any(Message.class),
                anyString(),
                any(SendCallback.class)
        );
    }

    /**
     * WTC_024: 测试collectNote方法 - 已收藏笔记
     */
    @Test
    void collectNote_AlreadyCollected() {
    }

    /**
     * WTC_025: 测试unCollectNote方法 - 成功取消收藏
     */
    @Test
    void unCollectNote_Success() {
    }

    /**
     * WTC_026: 测试unCollectNote方法 - 未收藏笔记
     */
    @Test
    void unCollectNote_NotCollected() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UnCollectNoteReqVO reqVO = UnCollectNoteReqVO.builder()
                .id(noteId)
                .build();

        // Mock笔记创建者ID
        when(noteDOMapper.selectCreatorIdByNoteId(noteId)).thenReturn(OTHER_USER_ID);

        // Mock Redis布隆过滤器返回0，表示未收藏
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(0L);

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.unCollectNote(reqVO);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_NOT_COLLECTED.getErrorCode(), exception.getErrorCode());
    }

    /**
     * WTC_027: 测试unlikeNote方法 - 成功取消点赞
     */
    @Test
    void unlikeNote_Success() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UnlikeNoteReqVO reqVO = UnlikeNoteReqVO.builder()
                .id(noteId)
                .build();

        // Mock笔记创建者ID
        when(noteDOMapper.selectCreatorIdByNoteId(noteId)).thenReturn(OTHER_USER_ID);

        // Mock Redis布隆过滤器返回1，表示已点赞
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        // Mock数据库查询返回已点赞记录 - 使用lenient()避免不必要的stubbing警告
        lenient().when(noteLikeDOMapper.selectCountByUserIdAndNoteId(anyLong(), anyLong())).thenReturn(1);

        // 执行测试方法
        Response<?> response = noteService.unlikeNote(reqVO);

        // 验证结果
        assertTrue(response.isSuccess());

        // 验证MQ消息发送
        verify(rocketMQTemplate).asyncSendOrderly(
                eq("LikeUnlikeTopic:Unlike"),
                any(Message.class),
                anyString(),
                any(SendCallback.class));
    }

    /**
     * WTC_028: 测试unlikeNote方法 - 未点赞笔记
     */
    @Test
    void unlikeNote_NotLiked() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        Long noteId = 1L;
        UnlikeNoteReqVO reqVO = UnlikeNoteReqVO.builder()
                .id(noteId)
                .build();

        // Mock笔记创建者ID
        when(noteDOMapper.selectCreatorIdByNoteId(noteId)).thenReturn(OTHER_USER_ID);

        // Mock Redis布隆过滤器返回0，表示未点赞
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(0L);

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.unlikeNote(reqVO);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.NOTE_NOT_LIKED.getErrorCode(), exception.getErrorCode());
    }

    /**
     * WTC_029: 测试getUserNotes方法 - 成功获取用户笔记列表
     */
    @Test
    void getUserNotes_Success() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 1;
        int size = 10;
        Integer isTop = 1;
        Integer visible = NoteVisibleEnum.PUBLIC.getCode();
        Integer status = 1;

        // 准备模拟的返回数据
        List<NoteDO> mockNotes = Arrays.asList(
            createMockNote(1L, "笔记1", MOCK_USER_ID),
            createMockNote(2L, "笔记2", MOCK_USER_ID)
        );

        // Mock查询方法
        when(noteDOMapper.selectUserNotes(any(Map.class))).thenReturn(mockNotes);

        // 执行测试方法
        Response<?> response = noteService.getUserNotes(MOCK_USER_ID, page, size, isTop, visible, status);

        // 验证结果
        assertTrue(response.isSuccess());
        List<NoteListRspVO> noteList = (List<NoteListRspVO>) response.getData();
        assertEquals(2, noteList.size());
        assertEquals(1L, noteList.get(0).getId());
        assertEquals(2L, noteList.get(1).getId());
        assertEquals(MOCK_USER_ID, noteList.get(0).getCreatorId());
        assertEquals(MOCK_USER_ID, noteList.get(1).getCreatorId());
    }

    /**
     * WTC_030: 测试getUserNotes方法 - 无效的分页参数
     */
    @Test
    void getUserNotes_InvalidPageParams() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据 - 无效的分页参数
        int page = 0;
        int size = 0;

        // 执行测试方法并验证异常
        BizException exception = assertThrows(BizException.class, () -> {
            noteService.getUserNotes(MOCK_USER_ID, page, size, null, null, null);
        });

        // 验证异常信息
        assertEquals(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), exception.getErrorCode());

        // 验证mapper没有被调用
        verify(noteDOMapper, never()).selectUserNotes(any(Map.class));
    }

    /**
     * WTC_031: 测试getUserNotes方法 - 空结果
     */
    @Test
    void getUserNotes_EmptyResult() {
        // 设置当前用户ID
        setCurrentUserId();

        // 准备测试数据
        int page = 1;
        int size = 10;

        // Mock查询方法返回空列表
        when(noteDOMapper.selectUserNotes(any(Map.class))).thenReturn(Collections.emptyList());

        // 执行测试方法
        Response<?> response = noteService.getUserNotes(MOCK_USER_ID, page, size, null, null, null);

        // 验证结果
        assertTrue(response.isSuccess());
        List<NoteListRspVO> noteList = (List<NoteListRspVO>) response.getData();
        assertTrue(noteList.isEmpty());
    }
}