package com.dealit.dealit.domain.member.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "member_interest_category",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_interest_category_member_category", columnNames = {"member_id", "category_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
	name = "member_interest_category_seq_generator",
	sequenceName = "member_interest_category_seq",
	allocationSize = 1
)
public class MemberInterestCategory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_interest_category_seq_generator")
	@Column(name = "member_interest_category_id")
	private Long memberInterestCategoryId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	private MemberInterestCategory(Long memberId, Long categoryId) {
		this.memberId = memberId;
		this.categoryId = categoryId;
	}

	public static MemberInterestCategory create(Long memberId, Long categoryId) {
		return new MemberInterestCategory(memberId, categoryId);
	}
}
