-- depth 1: root categories
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (1, '전자기기', 'Electronics', 1, NULL);

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (2, '의류', 'Fashion', 1, NULL);

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (3, '도서', 'Books', 1, NULL);

-- depth 2: children of 전자기기
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (4, '스마트폰', 'Smartphone', 2, (SELECT id FROM category WHERE name_ko = '전자기기' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (5, '노트북', 'Laptop', 2, (SELECT id FROM category WHERE name_ko = '전자기기' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (6, '태블릿', 'Tablet', 2, (SELECT id FROM category WHERE name_ko = '전자기기' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (7, '주변기기', 'Accessories', 2, (SELECT id FROM category WHERE name_ko = '전자기기' AND depth = 1));

-- depth 2: children of 의류
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (8, '상의', 'Tops', 2, (SELECT id FROM category WHERE name_ko = '의류' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (9, '하의', 'Bottoms', 2, (SELECT id FROM category WHERE name_ko = '의류' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (10, '아우터', 'Outerwear', 2, (SELECT id FROM category WHERE name_ko = '의류' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (11, '신발', 'Shoes', 2, (SELECT id FROM category WHERE name_ko = '의류' AND depth = 1));

-- depth 2: children of 도서
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (12, '전공서적', 'Major', 2, (SELECT id FROM category WHERE name_ko = '도서' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (13, '소설', 'Novel', 2, (SELECT id FROM category WHERE name_ko = '도서' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (14, '자격증', 'Certificate', 2, (SELECT id FROM category WHERE name_ko = '도서' AND depth = 1));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (15, '기타', 'Others', 2, (SELECT id FROM category WHERE name_ko = '도서' AND depth = 1));

-- depth 3: children of 스마트폰
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (16, '아이폰', 'iPhone', 3, (SELECT id FROM category WHERE name_ko = '스마트폰' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (17, '갤럭시', 'Galaxy', 3, (SELECT id FROM category WHERE name_ko = '스마트폰' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (18, '기타스마트폰', 'Other Smartphone', 3, (SELECT id FROM category WHERE name_ko = '스마트폰' AND depth = 2));

-- depth 3: children of 노트북
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (19, '맥북', 'MacBook', 3, (SELECT id FROM category WHERE name_ko = '노트북' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (20, '게이밍노트북', 'Gaming Laptop', 3, (SELECT id FROM category WHERE name_ko = '노트북' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (21, '사무용노트북', 'Office Laptop', 3, (SELECT id FROM category WHERE name_ko = '노트북' AND depth = 2));

-- depth 3: children of 태블릿
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (22, '아이패드', 'iPad', 3, (SELECT id FROM category WHERE name_ko = '태블릿' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (23, '갤럭시탭', 'Galaxy Tab', 3, (SELECT id FROM category WHERE name_ko = '태블릿' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (24, '기타태블릿', 'Other Tablet', 3, (SELECT id FROM category WHERE name_ko = '태블릿' AND depth = 2));

-- depth 3: children of 주변기기
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (25, '키보드', 'Keyboard', 3, (SELECT id FROM category WHERE name_ko = '주변기기' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (26, '마우스', 'Mouse', 3, (SELECT id FROM category WHERE name_ko = '주변기기' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (27, '이어폰', 'Earphone', 3, (SELECT id FROM category WHERE name_ko = '주변기기' AND depth = 2));

-- depth 3: children of 상의
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (28, '반팔', 'T-shirt', 3, (SELECT id FROM category WHERE name_ko = '상의' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (29, '셔츠', 'Shirt', 3, (SELECT id FROM category WHERE name_ko = '상의' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (30, '후드티', 'Hoodie', 3, (SELECT id FROM category WHERE name_ko = '상의' AND depth = 2));

-- depth 3: children of 하의
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (31, '청바지', 'Jeans', 3, (SELECT id FROM category WHERE name_ko = '하의' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (32, '슬랙스', 'Slacks', 3, (SELECT id FROM category WHERE name_ko = '하의' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (33, '반바지', 'Shorts', 3, (SELECT id FROM category WHERE name_ko = '하의' AND depth = 2));

-- depth 3: children of 아우터
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (34, '패딩', 'Padding', 3, (SELECT id FROM category WHERE name_ko = '아우터' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (35, '코트', 'Coat', 3, (SELECT id FROM category WHERE name_ko = '아우터' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (36, '자켓', 'Jacket', 3, (SELECT id FROM category WHERE name_ko = '아우터' AND depth = 2));

-- depth 3: children of 신발
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (37, '운동화', 'Sneakers', 3, (SELECT id FROM category WHERE name_ko = '신발' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (38, '구두', 'Dress Shoes', 3, (SELECT id FROM category WHERE name_ko = '신발' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (39, '슬리퍼', 'Slippers', 3, (SELECT id FROM category WHERE name_ko = '신발' AND depth = 2));

-- depth 3: children of 전공서적
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (40, '컴퓨터공학', 'Computer Science', 3, (SELECT id FROM category WHERE name_ko = '전공서적' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (41, '경영학', 'Business', 3, (SELECT id FROM category WHERE name_ko = '전공서적' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (42, '디자인', 'Design', 3, (SELECT id FROM category WHERE name_ko = '전공서적' AND depth = 2));

-- depth 3: children of 소설
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (43, '장편소설', 'Fiction', 3, (SELECT id FROM category WHERE name_ko = '소설' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (44, '단편소설', 'Short Story', 3, (SELECT id FROM category WHERE name_ko = '소설' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (45, '판타지', 'Fantasy', 3, (SELECT id FROM category WHERE name_ko = '소설' AND depth = 2));

-- depth 3: children of 자격증
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (46, '개발', 'Development', 3, (SELECT id FROM category WHERE name_ko = '자격증' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (47, '어학', 'Language', 3, (SELECT id FROM category WHERE name_ko = '자격증' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (48, '공무원', 'Civil Service', 3, (SELECT id FROM category WHERE name_ko = '자격증' AND depth = 2));

-- depth 3: children of 기타
INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (49, '만화', 'Comic', 3, (SELECT id FROM category WHERE name_ko = '기타' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (50, '잡지', 'Magazine', 3, (SELECT id FROM category WHERE name_ko = '기타' AND depth = 2));

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES (51, '문제집', 'Workbook', 3, (SELECT id FROM category WHERE name_ko = '기타' AND depth = 2));

ALTER TABLE category ALTER COLUMN id RESTART WITH 52;
