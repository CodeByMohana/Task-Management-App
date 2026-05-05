package com.app.taskmanagement.boardservice.dto;

import com.app.taskmanagement.boardservice.entity.Board;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for updating an existing board.
 * All fields are optional — only non-null fields are updated (PATCH-style).
 */
@Data
public class UpdateBoardRequest {

    @Size(min = 2, max = 100)
    private String name;

    private String description;
    private String background;
    private Board.Visibility visibility;
}