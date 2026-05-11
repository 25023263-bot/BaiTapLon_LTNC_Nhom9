package com.nhom9.auction.baitaplon_ltnc_nhom9.domain;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.factory.ItemFactory;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Art;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Electronics;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Item;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    // Test tạo Electronics item
    @Test
    void testCreateElectronics() {

        Item item = ItemFactory.createItem(
                "electronics",
                "E01",
                "Laptop",
                1000,
                "Gaming laptop"
        );

        assertNotNull(item);

        assertTrue(item instanceof Electronics);

        assertEquals("E01", item.getItemId());

        assertEquals("Laptop", item.getName());

        assertEquals(1000, item.getStartingPrice());
    }

    // Test tạo Art item
    @Test
    void testCreateArt() {

        Item item = ItemFactory.createItem(
                "art",
                "A01",
                "Mona Lisa",
                5000,
                "Famous painting"
        );

        assertNotNull(item);

        assertTrue(item instanceof Art);

        assertEquals("A01", item.getItemId());

        assertEquals("Mona Lisa", item.getName());

        assertEquals(5000, item.getStartingPrice());
    }

    // Test tạo Vehicle item
    @Test
    void testCreateVehicle() {

        Item item = ItemFactory.createItem(
                "vehicle",
                "V01",
                "Toyota",
                20000,
                "Car"
        );

        assertNotNull(item);

        assertTrue(item instanceof Vehicle);

        assertEquals("V01", item.getItemId());

        assertEquals("Toyota", item.getName());

        assertEquals(20000, item.getStartingPrice());
    }

    // Test nhập type không hợp lệ
    @Test
    void testInvalidItemType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ItemFactory.createItem(
                        "invalidType",
                        "X01",
                        "Unknown",
                        100,
                        "Invalid item"
                )
        );
    }

    // Test không phân biệt chữ hoa chữ thường
    @Test
    void testCaseInsensitiveType() {

        Item item = ItemFactory.createItem(
                "ELECTRONICS",
                "E02",
                "Phone",
                800,
                "Smartphone"
        );

        assertTrue(item instanceof Electronics);
    }
}