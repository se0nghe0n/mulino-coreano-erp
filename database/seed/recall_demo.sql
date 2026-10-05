-- 재보충 fixture 위에 중간제품 2단계와 중복 고객 출하를 더한다. 운영 DB용 seed가 아니다.
DO $$ DECLARE plant BIGINT; actor BIGINT; product BIGINT; source BIGINT; middle BIGINT; final BIGINT; record BIGINT; customer BIGINT; original BIGINT; order_ref BIGINT; outbound_ref BIGINT; unrelated BIGINT; pack BIGINT; other_product BIGINT; n INT;
BEGIN
 SELECT warehouse_id INTO plant FROM warehouses WHERE plant_id='DEMO-KR-01';
 SELECT user_id INTO actor FROM users WHERE email='replenishment-demo@example.invalid';
 SELECT product_id,production_lot_id INTO product,source FROM production_lots WHERE lot_number='DEMO-DOUGH-CURRENT';
 SELECT customer_id INTO original FROM customers WHERE name='DEMO 국내 고객';
 INSERT INTO production_lots(product_id,warehouse_id,lot_number,production_date,expiry_date,quantity) VALUES(product,plant,'RC-INTERMEDIATE','2026-09-02','2026-12-31',2) RETURNING production_lot_id INTO middle;
 INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) VALUES(middle,plant,actor,'MIX','2026-09-02 10:00') RETURNING production_record_id INTO record;
 INSERT INTO production_product_inputs(production_record_id,source_production_lot_id,quantity_used) VALUES(record,source,0.5);
 INSERT INTO production_lots(product_id,warehouse_id,lot_number,production_date,expiry_date,quantity) VALUES(product,plant,'RC-FINAL','2026-09-03','2026-12-31',3) RETURNING production_lot_id INTO final;
 INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) VALUES(final,plant,actor,'PACK','2026-09-03 10:00') RETURNING production_record_id INTO record;
 INSERT INTO production_product_inputs(production_record_id,source_production_lot_id,quantity_used) VALUES(record,middle,0.5);
 INSERT INTO customers(name,address) VALUES('RC 두 번째 고객','KR') RETURNING customer_id INTO customer;
 FOR n IN 1..3 LOOP
 INSERT INTO orders(customer_id,created_by,order_date,status) VALUES(CASE WHEN n=3 THEN customer ELSE original END,actor,'2026-09-04','SHIPPED') RETURNING order_id INTO order_ref;
 INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(order_ref,product,0.25,1);
 INSERT INTO outbound(product_id,warehouse_id,order_id,quantity,outbound_date) VALUES(product,plant,order_ref,0.25,'2026-09-04') RETURNING outbound_id INTO outbound_ref;
 INSERT INTO outbound_lots(outbound_id,lot_id,lot_quantity) VALUES(outbound_ref,final,0.25);
 END LOOP;
 UPDATE stock SET quantity=7.25 WHERE product_id=product AND warehouse_id=plant;
 -- 포장재는 영향 LOT의 co-input 증거다. 포장재만 쓴 별도 LOT은 사고 조상이 아니다.
 SELECT raw_material_lot_id INTO pack FROM raw_material_lots WHERE lot_number='DEMO-RM-PACK';
 SELECT product_id INTO other_product FROM products WHERE sku='DEMO-AMR';
 INSERT INTO production_lots(product_id,warehouse_id,lot_number,production_date,expiry_date,quantity) VALUES(other_product,plant,'RC-UNRELATED-PACK','2026-09-04','2026-12-31',1) RETURNING production_lot_id INTO unrelated;
 INSERT INTO production_records(lot_id,warehouse_id,operator_id,process_type,start_time) VALUES(unrelated,plant,actor,'PACK','2026-09-04 10:00') RETURNING production_record_id INTO record;
 INSERT INTO production_ingredients(production_record_id,raw_material_lot_id,quantity_used) VALUES(record,pack,0.5);
 INSERT INTO customers(name,address) VALUES('RC 무관한 고객','KR') RETURNING customer_id INTO customer;
 INSERT INTO orders(customer_id,created_by,order_date,status) VALUES(customer,actor,'2026-09-04','SHIPPED') RETURNING order_id INTO order_ref;
 INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(order_ref,other_product,0.25,1);
 INSERT INTO outbound(product_id,warehouse_id,order_id,quantity,outbound_date) VALUES(other_product,plant,order_ref,0.25,'2026-09-04') RETURNING outbound_id INTO outbound_ref;
 INSERT INTO outbound_lots(outbound_id,lot_id,lot_quantity) VALUES(outbound_ref,unrelated,0.25);
 UPDATE stock SET quantity=40.75 WHERE product_id=other_product AND warehouse_id=plant;
END $$;
