package com.dealit.dealit.domain.member.dto;

public record MyPageProfileResponse(
	Long id,
	String name,
	String nickname,
	String email,
	String bio,
	String profileImageUrl,
	String location,
	boolean verified,
	double rating,
	int warningCount,
	int biddingCount,
	int sellingCount,
	int wishlistCount
) {
}
