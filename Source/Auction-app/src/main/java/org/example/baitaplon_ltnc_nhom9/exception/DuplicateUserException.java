package org.example.baitaplon_ltnc_nhom9.exception;

/**
 * Ném ra khi username hoặc email đã tồn tại trong hệ thống.
 */
public class DuplicateUserException extends Exception {

    public enum Field { USERNAME, EMAIL }

    private final Field duplicateField;
    private final String value;

    public DuplicateUserException(Field field, String value) {
        super(field == Field.USERNAME
                ? "Tên đăng nhập '" + value + "' đã được sử dụng."
                : "Email '" + value + "' đã được đăng ký.");
        this.duplicateField = field;
        this.value          = value;
    }

    public Field getDuplicateField() { return duplicateField; }
    public String getValue()         { return value; }
}