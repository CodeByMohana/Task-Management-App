package com.app.taskmanagement.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.taskmanagement.workspace.entity.Workspace;

/**
 * Data access layer for Workspace.
 *
 * Spring Data JPA automatically generates SQL for these method signatures. You
 * don't need to write any SQL — Spring reads the method name and figures it
 * out.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Integer> {

	/**
	 * Find all workspaces where a specific user is a member. Uses a JOIN between
	 * workspaces and workspace_members tables.
	 *
	 * JPQL (Java Persistence Query Language) is like SQL but uses entity/field
	 * names, not table/column names.
	 */
	@Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.userId = :userId")
	List<Workspace> findAllByMemberUserId(@Param("userId") int userId);

	/**
	 * Find all PUBLIC workspaces — for the discovery/browse feature.
	 */
	@Query("SELECT w FROM Workspace w WHERE w.visibility = 'PUBLIC'")
	List<Workspace> findAllPublic();

	/**
	 * Find all workspaces created by a specific user.
	 */
	List<Workspace> findAllByCreatedByUserId(int userId);

	/**
	 * Search public workspaces by name — useful for a search bar.
	 * 'ContainingIgnoreCase' = SQL LIKE '%name%' (case-insensitive).
	 */
	List<Workspace> findByNameContainingIgnoreCaseAndVisibility(String name, Workspace.Visibility visibility);
}