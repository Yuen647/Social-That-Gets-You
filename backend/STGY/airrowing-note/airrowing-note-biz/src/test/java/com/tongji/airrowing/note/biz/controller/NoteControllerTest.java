package com.tongji.airrowing.note.biz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongji.airrowing.note.biz.model.vo.*;
import com.tongji.airrowing.note.biz.service.NoteService;
import com.tongji.framework.common.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteController noteController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(noteController).build();
        objectMapper.findAndRegisterModules(); // 支持Java 8日期时间类型
    }

    @Test
    void publishNote() throws Exception {
        // 准备测试数据
        PublishNoteReqVO reqVO = PublishNoteReqVO.builder()
                .type(1) // 图文类型
                .imgUris(Arrays.asList("image1.jpg", "image2.jpg"))
                .title("测试笔记")
                .content("这是一篇测试笔记内容")
                .topicId(1L)
                .build();

        // Mock服务响应
        when(noteService.publishNote(any(PublishNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void findNoteDetail() throws Exception {
        // 准备测试数据
        FindNoteDetailReqVO reqVO = FindNoteDetailReqVO.builder()
                .id(1L)
                .build();

        FindNoteDetailRspVO rspVO = FindNoteDetailRspVO.builder()
                .id(1L)
                .creatorId(1001L)
                .creatorName("测试用户")
                .avatar("avatar.jpg")
                .title("测试笔记")
                .content("这是测试内容")
                .imgUris(Arrays.asList("image1.jpg"))
                .build();

        // Mock服务响应
        when(noteService.findNoteDetail(any(FindNoteDetailReqVO.class))).thenReturn(Response.success(rspVO));

        // 执行请求并验证结果
        mockMvc.perform(post("/note/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试笔记"));
    }

    @Test
    void updateNote() throws Exception {
        // 准备测试数据
        UpdateNoteReqVO reqVO = UpdateNoteReqVO.builder()
                .id(1L)
                .type(1)
                .title("更新后的标题")
                .content("更新后的内容")
                .imgUris(Arrays.asList("newImage.jpg"))
                .build();

        // Mock服务响应
        when(noteService.updateNote(any(UpdateNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteNote() throws Exception {
        // 准备测试数据
        DeleteNoteReqVO reqVO = DeleteNoteReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.deleteNote(any(DeleteNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void visibleOnlyMe() throws Exception {
        // 准备测试数据
        UpdateNoteVisibleOnlyMeReqVO reqVO = UpdateNoteVisibleOnlyMeReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.visibleOnlyMe(any(UpdateNoteVisibleOnlyMeReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/visible/onlyme")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void topNote() throws Exception {
        // 准备测试数据
        TopNoteReqVO reqVO = TopNoteReqVO.builder()
                .id(1L)
                .isTop(true)
                .build();

        // Mock服务响应
        when(noteService.topNote(any(TopNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/top")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void likeNote() throws Exception {
        // 准备测试数据
        LikeNoteReqVO reqVO = LikeNoteReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.likeNote(any(LikeNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/like")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unlikeNote() throws Exception {
        // 准备测试数据
        UnlikeNoteReqVO reqVO = UnlikeNoteReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.unlikeNote(any(UnlikeNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/unlike")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void collectNote() throws Exception {
        // 准备测试数据
        CollectNoteReqVO reqVO = CollectNoteReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.collectNote(any(CollectNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unCollectNote() throws Exception {
        // 准备测试数据
        UnCollectNoteReqVO reqVO = UnCollectNoteReqVO.builder()
                .id(1L)
                .build();

        // Mock服务响应
        when(noteService.unCollectNote(any(UnCollectNoteReqVO.class))).thenReturn(Response.success());

        // 执行请求并验证结果
        mockMvc.perform(post("/note/uncollect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getNoteList() throws Exception {
        // 准备测试数据
        NoteListReqVO reqVO = new NoteListReqVO();
        reqVO.setPage(1);
        reqVO.setSize(10);

        List<NoteListRspVO> noteList = Arrays.asList(
                new NoteListRspVO(1L, "笔记1", 1001L),
                new NoteListRspVO(2L, "笔记2", 1002L)
        );

        // Mock服务响应 - 使用原始类型避免泛型问题
        Response response = Response.success(noteList);
        when(noteService.getNoteList(anyInt(), anyInt())).thenReturn(response);

        // 执行请求并验证结果
        mockMvc.perform(post("/note/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserNoteList() throws Exception {
        // 准备测试数据
        GetUserNotesRequest reqVO = new GetUserNotesRequest();
        reqVO.setUserId(1001L);
        reqVO.setPage(1);
        reqVO.setSize(10);

        List<NoteListRspVO> noteList = Arrays.asList(
                new NoteListRspVO(1L, "用户笔记1", 1001L),
                new NoteListRspVO(2L, "用户笔记2", 1001L)
        );

        // Mock服务响应 - 使用原始类型避免泛型问题
        Response response = Response.success(noteList);
        when(noteService.getUserNotes(anyLong(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(response);

        // 执行请求并验证结果
        mockMvc.perform(post("/note/UserNoteList")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void getUserLikedNotes() throws Exception {
        // 准备测试数据
        GetUserLikedNotesRequest reqVO = new GetUserLikedNotesRequest();
        reqVO.setUserId(1001L);
        reqVO.setPage(1);
        reqVO.setSize(10);

        List<LikedNoteVO> likedNotes = Arrays.asList(
                LikedNoteVO.builder().id(1L).title("点赞笔记1").creatorId(1002L).build(),
                LikedNoteVO.builder().id(2L).title("点赞笔记2").creatorId(1003L).build()
        );

        // Mock服务响应
        Response<List<LikedNoteVO>> response = Response.success(likedNotes);
        when(noteService.getUserLikedNotes(anyLong(), anyInt(), anyInt()))
                .thenReturn(response);

        // 执行请求并验证结果
        mockMvc.perform(post("/note/liked/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void getUserCollectedNotes() throws Exception {
        // 准备测试数据
        GetUserCollectedNotesRequest reqVO = new GetUserCollectedNotesRequest();
        reqVO.setUserId(1001L);
        reqVO.setPage(1);
        reqVO.setSize(10);

        List<CollectedNoteVO> collectedNotes = Arrays.asList(
                CollectedNoteVO.builder().id(1L).title("收藏笔记1").creatorId(1002L).build(),
                CollectedNoteVO.builder().id(2L).title("收藏笔记2").creatorId(1003L).build()
        );

        // Mock服务响应
        Response<List<CollectedNoteVO>> response = Response.success(collectedNotes);
        when(noteService.getUserCollectedNotes(anyLong(), anyInt(), anyInt()))
                .thenReturn(response);

        // 执行请求并验证结果
        mockMvc.perform(post("/note/collected/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }
}