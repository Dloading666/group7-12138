package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    
    private final TaskRepository taskRepository;
    
    /**
     * 测试任务数据
     */
    @GetMapping("/tasks")
    public ApiResponse<List<Task>> testTasks() {
        List<Task> tasks = taskRepository.findAll();
        System.out.println("========== 任务数量: " + tasks.size() + " ==========");
        for (Task task : tasks) {
            System.out.println("任务: " + task.getTaskId() + " - " + task.getName() + " - " + task.getStatus());
        }
        return ApiResponse.success(tasks);
    }
    
    /**
     * 检查数据库表是否存在
     */
    @GetMapping("/check-table")
    public ApiResponse<String> checkTable() {
        try {
            long count = taskRepository.count();
            return ApiResponse.success("任务表存在，记录数: " + count);
        } catch (Exception e) {
            return ApiResponse.error("任务表不存在或查询失败: " + e.getMessage());
        }
    }
}
