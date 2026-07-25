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
public class users {
	@Id
	private UUID id;
	private String f_name;
	private String l_name;
	private String email;
	@Column(unique = true)
	private String password;
	private Boolean is_admin;
	private Boolean is_active;
	private LocalDateTime created_at;
	private LocalDateTime updated_at;

	@PrePersist
	public void prePersist() {
		updated_at = LocalDateTime.now();
		created_at = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		updated_at = LocalDateTime.now();
	}

}

