package com.dealit.dealit.domain.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	@Id
	private Long id;

	@Column(name = "name_ko", nullable = false, length = 100)
	private String nameKo;

	@Column(name = "name_en", nullable = false, length = 100)
	private String nameEn;

	@Column(name = "depth", nullable = false)
	private Integer depth;

	@Column(name = "parent_id")
	private Long parentId;
}
