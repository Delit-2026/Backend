INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타노트북', 'Other Laptop', 3, id
FROM category
WHERE name_ko = '노트북' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타노트북' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타주변기기', 'Other Accessories', 3, id
FROM category
WHERE name_ko = '주변기기' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타주변기기' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타상의', 'Other Tops', 3, id
FROM category
WHERE name_ko = '상의' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타상의' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타하의', 'Other Bottoms', 3, id
FROM category
WHERE name_ko = '하의' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타하의' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타아우터', 'Other Outerwear', 3, id
FROM category
WHERE name_ko = '아우터' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타아우터' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타신발', 'Other Shoes', 3, id
FROM category
WHERE name_ko = '신발' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타신발' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타전공서적', 'Other Major Books', 3, id
FROM category
WHERE name_ko = '전공서적' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타전공서적' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타소설', 'Other Novels', 3, id
FROM category
WHERE name_ko = '소설' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타소설' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타자격증', 'Other Certificates', 3, id
FROM category
WHERE name_ko = '자격증' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타자격증' AND depth = 3);

INSERT INTO category (name_ko, name_en, depth, parent_id)
SELECT '기타도서', 'Other Books', 3, id
FROM category
WHERE name_ko = '기타' AND depth = 2
  AND NOT EXISTS (SELECT 1 FROM category WHERE name_ko = '기타도서' AND depth = 3);
