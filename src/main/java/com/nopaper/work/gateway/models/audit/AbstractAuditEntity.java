package com.nopaper.work.gateway.models.audit;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nopaper.work.gateway.constants.GatewayConstant;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractAuditEntity implements Serializable {

	private static final long serialVersionUID = -4026971765558491799L;

	@CreatedBy
	@Column(value = "created_by")
	@JsonIgnore
	private String createdBy = GatewayConstant.DEFAULT_ACTOR;

	@CreatedDate
	@Column(value = "created_date")
	@JsonIgnore
	private Instant createdDate = Instant.now();

	@LastModifiedBy
	@Column(value = "last_modified_by")
	@JsonIgnore
	private String lastModifiedBy = GatewayConstant.DEFAULT_ACTOR;

	@NotNull
	@Column(value = "status")
	@JsonIgnore
	private String status = GatewayConstant.DEFAULT_STATUS;

	@LastModifiedDate
	@Column(value = "last_modified_date")
	@JsonIgnore
	private Instant lastModifiedDate = Instant.now();
}
