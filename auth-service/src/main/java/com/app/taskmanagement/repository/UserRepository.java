package com.app.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.taskmanagement.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	Optional<User> findByProviderAndProviderId(String provider, String providerId);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	List<User> findAllByRole(User.Role role);

	List<User> findByFullNameContainingIgnoreCase(String fullName);

	void deleteByUserId(int userId);
}