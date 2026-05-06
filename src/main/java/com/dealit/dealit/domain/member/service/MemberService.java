package com.dealit.dealit.domain.member.service;

import com.dealit.dealit.domain.auction.entity.Category;
import com.dealit.dealit.domain.auction.repository.CategoryRepository;
import com.dealit.dealit.domain.member.dto.InterestCategoryOptionResponse;
import com.dealit.dealit.domain.member.dto.LoginIdCheckResponse;
import com.dealit.dealit.domain.member.dto.NicknameCheckResponse;
import com.dealit.dealit.domain.member.dto.SignUpRequest;
import com.dealit.dealit.domain.member.dto.SignUpResponse;
import com.dealit.dealit.domain.member.entity.Member;
import com.dealit.dealit.domain.member.entity.MemberInterestCategory;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import com.dealit.dealit.domain.member.exception.InvalidInterestCategoryRequestException;
import com.dealit.dealit.domain.member.repository.MemberInterestCategoryRepository;
import com.dealit.dealit.domain.member.repository.MemberRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberInterestCategoryRepository memberInterestCategoryRepository;
	private final CategoryRepository categoryRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerificationService;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		validateDuplicate(request);
		List<Category> interestCategories = loadInterestCategories(request.interestCategoryIds());

		String normalizedEmail = normalizeEmail(request.email());
		boolean verified = normalizedEmail != null
			&& emailVerificationService.consumeVerifiedStatus(normalizedEmail);

		Member member = Member.create(
			request.loginId().trim(),
			passwordEncoder.encode(request.password()),
			normalizedEmail,
			null,
			normalizeBlank(request.name()),
			verified
		);

		Member savedMember = memberRepository.save(member);
		savedMember.assignDefaultNickname();
		saveInterestCategories(savedMember, interestCategories);

		return new SignUpResponse(
			savedMember.getMemberId(),
			savedMember.getLoginId(),
			savedMember.getEmail(),
			savedMember.getNickname(),
			savedMember.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public List<InterestCategoryOptionResponse> getInterestCategories() {
		return categoryRepository.findAllByDepthOrderByIdAsc(1).stream()
			.map(category -> new InterestCategoryOptionResponse(
				category.getId(),
				category.getNameKo(),
				category.getNameEn()
			))
			.toList();
	}

	@Transactional(readOnly = true)
	public LoginIdCheckResponse checkLoginIdAvailability(String loginId) {
		String normalizedLoginId = loginId.trim();
		return new LoginIdCheckResponse(
			normalizedLoginId,
			!memberRepository.existsByLoginId(normalizedLoginId)
		);
	}

	@Transactional(readOnly = true)
	public NicknameCheckResponse checkNicknameAvailability(String nickname) {
		String normalizedNickname = nickname.trim();
		return new NicknameCheckResponse(
			normalizedNickname,
			!memberRepository.existsByNickname(normalizedNickname)
		);
	}

	private void validateDuplicate(SignUpRequest request) {
		String normalizedEmail = normalizeEmail(request.email());
		String normalizedLoginId = request.loginId().trim();

		if (normalizedEmail != null && memberRepository.existsByEmail(normalizedEmail)) {
			throw new DuplicateMemberException("이미 가입된 이메일입니다.");
		}

		if (memberRepository.existsByLoginId(normalizedLoginId)) {
			throw new DuplicateMemberException("이미 사용 중인 로그인 아이디입니다.");
		}
	}

	private String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeEmail(String email) {
		String normalized = normalizeBlank(email);
		return normalized == null ? null : normalized.toLowerCase();
	}

	private List<Category> loadInterestCategories(List<Long> interestCategoryIds) {
		if (interestCategoryIds == null || interestCategoryIds.isEmpty()) {
			return List.of();
		}

		Set<Long> uniqueIds = new LinkedHashSet<>();
		for (Long interestCategoryId : interestCategoryIds) {
			if (interestCategoryId == null || interestCategoryId <= 0L) {
				throw new InvalidInterestCategoryRequestException("관심 카테고리 ID가 올바르지 않습니다.");
			}
			if (!uniqueIds.add(interestCategoryId)) {
				throw new InvalidInterestCategoryRequestException("중복된 관심 카테고리는 선택할 수 없습니다.");
			}
		}

		List<Category> categories = categoryRepository.findAllById(uniqueIds);
		if (categories.size() != uniqueIds.size()) {
			throw new InvalidInterestCategoryRequestException("존재하지 않는 관심 카테고리가 포함되어 있습니다.");
		}

		boolean hasNonTopLevelCategory = categories.stream()
			.anyMatch(category -> category.getDepth() == null || category.getDepth() != 1);
		if (hasNonTopLevelCategory) {
			throw new InvalidInterestCategoryRequestException("관심 카테고리는 대분류만 선택할 수 있습니다.");
		}

		return categories;
	}

	private void saveInterestCategories(Member member, List<Category> categories) {
		if (categories.isEmpty()) {
			return;
		}

		List<MemberInterestCategory> mappings = categories.stream()
			.map(category -> MemberInterestCategory.create(member.getMemberId(), category.getId()))
			.toList();
		memberInterestCategoryRepository.saveAll(mappings);
	}
}
