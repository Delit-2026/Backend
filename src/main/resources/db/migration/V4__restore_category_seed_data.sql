-- Restore category seed data for databases where V2 was marked as applied
-- but the category rows are missing.

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES
    (1, '전자기기', 'Electronics', 1, NULL),
    (2, '의류', 'Fashion', 1, NULL),
    (3, '도서', 'Books', 1, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES
    (4, '스마트폰', 'Smartphone', 2, 1),
    (5, '노트북', 'Laptop', 2, 1),
    (6, '태블릿', 'Tablet', 2, 1),
    (7, '주변기기', 'Accessories', 2, 1),
    (8, '상의', 'Tops', 2, 2),
    (9, '하의', 'Bottoms', 2, 2),
    (10, '아우터', 'Outerwear', 2, 2),
    (11, '신발', 'Shoes', 2, 2),
    (12, '전공서적', 'Major', 2, 3),
    (13, '소설', 'Novel', 2, 3),
    (14, '자격증', 'Certificate', 2, 3),
    (15, '기타', 'Others', 2, 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (id, name_ko, name_en, depth, parent_id)
VALUES
    (16, '아이폰', 'iPhone', 3, 4),
    (17, '갤럭시', 'Galaxy', 3, 4),
    (18, '기타스마트폰', 'Other Smartphone', 3, 4),
    (19, '맥북', 'MacBook', 3, 5),
    (20, '게이밍노트북', 'Gaming Laptop', 3, 5),
    (21, '사무용노트북', 'Office Laptop', 3, 5),
    (22, '아이패드', 'iPad', 3, 6),
    (23, '갤럭시탭', 'Galaxy Tab', 3, 6),
    (24, '기타태블릿', 'Other Tablet', 3, 6),
    (25, '키보드', 'Keyboard', 3, 7),
    (26, '마우스', 'Mouse', 3, 7),
    (27, '이어폰', 'Earphone', 3, 7),
    (28, '반팔', 'T-shirt', 3, 8),
    (29, '셔츠', 'Shirt', 3, 8),
    (30, '후드티', 'Hoodie', 3, 8),
    (31, '청바지', 'Jeans', 3, 9),
    (32, '슬랙스', 'Slacks', 3, 9),
    (33, '반바지', 'Shorts', 3, 9),
    (34, '패딩', 'Padding', 3, 10),
    (35, '코트', 'Coat', 3, 10),
    (36, '자켓', 'Jacket', 3, 10),
    (37, '운동화', 'Sneakers', 3, 11),
    (38, '구두', 'Dress Shoes', 3, 11),
    (39, '슬리퍼', 'Slippers', 3, 11),
    (40, '컴퓨터공학', 'Computer Science', 3, 12),
    (41, '경영학', 'Business', 3, 12),
    (42, '디자인', 'Design', 3, 12),
    (43, '장편소설', 'Fiction', 3, 13),
    (44, '단편소설', 'Short Story', 3, 13),
    (45, '판타지', 'Fantasy', 3, 13),
    (46, '개발', 'Development', 3, 14),
    (47, '어학', 'Language', 3, 14),
    (48, '공무원', 'Civil Service', 3, 14),
    (49, '만화', 'Comic', 3, 15),
    (50, '잡지', 'Magazine', 3, 15),
    (51, '문제집', 'Workbook', 3, 15)
ON CONFLICT (id) DO NOTHING;
