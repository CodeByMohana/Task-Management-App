package com.app.taskmanagement.boardservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kanban Board inside a Workspace.
 *
 * Hierarchy:
 *   Workspace → Board → List → Card
 *
 * A board contains an ordered set of Lists (columns).
 * Example board: "Sprint 1" with lists: "To Do", "In Progress", "Done"
 *
 * Key design decision:
 * We store workspaceId as a plain integer (not a @ManyToOne to Workspace entity)
 * because Workspace lives in a DIFFERENT database (workspace-service).
 * Microservices never share tables — they communicate by IDs.
 */
@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer boardId;

    /**
     * The workspace this board belongs to.
     * Stored as a plain ID — not a foreign key to workspace-service DB.
     */
    @Column(nullable = false)
    private Integer workspaceId;

    /**
     * Display name of the board.
     * Example: "Sprint 1", "Bug Tracker", "Marketing Campaign"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional description explaining the board's purpose.
     */
    private String description;

    /**
     * Background color or image URL for the board UI.
     * Example: "#0052CC" or "https://images.unsplash.com/..."
     */
    private String background;

    /**
     * Board visibility:
     * PUBLIC        → anyone can view (even without login)
     * PRIVATE       → only board members can view
     * WORKSPACE     → any member of the parent workspace can view
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.WORKSPACE;

    /**
     * A closed board is read-only — no new cards or changes allowed.
     * Useful for archiving a completed sprint without deleting it.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;

    /**
     * The userId of whoever created this board.
     * Stored as ID — user data lives in auth-service DB.
     */
    @Column(nullable = false)
    private Integer createdByUserId;

    /**
     * All lists (columns) on this board, ordered by their position field.
     * CascadeType.ALL — deleting a board also deletes all its lists.
     * orphanRemoval = true — removing a list from this collection deletes it from DB.
     */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")   // always return lists in correct left-to-right order
    @Builder.Default
    private List<BoardList> lists = new ArrayList<>();

    /**
     * All members of this board with their roles.
     */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Enum ────────────────────────────────────────────────────────────────

    public enum Visibility {
        PUBLIC,      // visible to everyone, even guests
        PRIVATE,     // only board members can see it
        WORKSPACE    // visible to all workspace members
    }
}