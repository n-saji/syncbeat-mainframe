package com.syncbeat.mainframe.syncbeatmainframe.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {
	@Id
	private UUID id;
	@Column(name = "f_name")
	private String firstName;
	@Column(name = "l_name")
	private String lastName;

	@Column(unique = true)
	private String email;
	
	private String password;

	@Column(name = "is_admin")
	private Boolean admin;

	@Column(name = "is_active")
	private Boolean active;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		updatedAt = LocalDateTime.now();
		createdAt = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

}

