package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho AuthService — đăng nhập, đăng ký, validation.
 *
 * <h3>Chiến lược test AuthService:</h3>
 * <p>AuthService phụ thuộc vào {@link UserRepository} và {@link PasswordHasher}.
 * PasswordHasher là static utility (không inject) nên ta không mock được —
 * thay vào đó ta dùng PasswordHasher.hash() thật để tạo test data.</p>
 *
 * <p>UserRepository được mock để tránh cần DB thật.</p>
 *
 * <h3>@ParameterizedTest là gì?</h3>
 * <p>Cho phép chạy một test với nhiều input khác nhau mà không cần viết
 * nhiều method. Ví dụ: test tất cả password yếu chỉ với 1 method.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Authentication & Registration Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepo;

    private AuthService authService;

    // Dữ liệu test dùng chung
    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_EMAIL    = "test@example.com";
    private static final String VALID_PASSWORD = "Password123"; // đủ mạnh: hoa + thường + số

    // Hash thật của VALID_PASSWORD — dùng BCrypt nên chậm hơn, chỉ tạo 1 lần
    private static final String VALID_PASSWORD_HASH = PasswordHasher.hash(VALID_PASSWORD);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo);
    }

    // =========================================================================
    // login() tests
    // =========================================================================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Happy path: username + password đúng → trả về User")
        void login_validCredentials_returnsUser() throws Exception {
            // ARRANGE — Tạo Buyer với password hash thật
            Buyer buyer = new Buyer(1, VALID_USERNAME, VALID_EMAIL,
                    VALID_PASSWORD_HASH, "Nguyen Van A", "0900000000");
            buyer.setActive(true);

            // Giả lập DB: tìm theo username → trả về buyer
            when(userRepo.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(buyer));

            // ACT
            User result = authService.login(VALID_USERNAME, VALID_PASSWORD);

            // ASSERT
            assertNotNull(result);
            assertEquals(VALID_USERNAME, result.getUsername());
            // AuthService KHÔNG được ghi UserSession — đó là việc của Controller
            // Nên ta chỉ kiểm tra return value, không kiểm tra side effect UI
        }

        @Test
        @DisplayName("Username không tồn tại → INVALID_CREDENTIALS")
        void login_usernameNotFound_throwsInvalidCredentials() throws Exception {
            when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login("ghost", VALID_PASSWORD));

            // Không tiết lộ "username không tồn tại" (tránh user enumeration attack)
            assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
        }

        @Test
        @DisplayName("Password sai → INVALID_CREDENTIALS")
        void login_wrongPassword_throwsInvalidCredentials() throws Exception {
            Buyer buyer = new Buyer(1, VALID_USERNAME, VALID_EMAIL,
                    VALID_PASSWORD_HASH, "Test User", "0900000000");
            buyer.setActive(true);

            when(userRepo.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(buyer));

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login(VALID_USERNAME, "WrongPass999"));

            assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, ex.getReason());
        }

        @Test
        @DisplayName("Tài khoản bị khóa (isActive = false) → ACCOUNT_DISABLED")
        void login_disabledAccount_throwsAccountDisabled() throws Exception {
            Buyer buyer = new Buyer(1, VALID_USERNAME, VALID_EMAIL,
                    VALID_PASSWORD_HASH, "Test User", "0900000000");
            buyer.setActive(false); // tài khoản bị khóa

            when(userRepo.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(buyer));

            AuthenticationException ex = assertThrows(AuthenticationException.class,
                    () -> authService.login(VALID_USERNAME, VALID_PASSWORD));

            assertEquals(AuthenticationException.Reason.ACCOUNT_DISABLED, ex.getReason());
        }

        @Test
        @DisplayName("Username null → INVALID_CREDENTIALS (không crash)")
        void login_nullUsername_throwsInvalidCredentials() throws Exception {
            assertThrows(AuthenticationException.class,
                    () -> authService.login(null, VALID_PASSWORD));

            // userRepo không được gọi nếu input null
            verify(userRepo, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Password rỗng → INVALID_CREDENTIALS (không crash)")
        void login_blankPassword_throwsInvalidCredentials() throws Exception {
            assertThrows(AuthenticationException.class,
                    () -> authService.login(VALID_USERNAME, "   "));

            verify(userRepo, never()).findByUsername(any());
        }

        @Test
        @DisplayName("Login đúng không ghi UserSession (Service độc lập với UI)")
        void login_success_doesNotTouchUserSession() throws Exception {
            // Test này đảm bảo AuthService không vi phạm nguyên tắc:
            // "Service không biết UI tồn tại"
            Buyer buyer = new Buyer(1, VALID_USERNAME, VALID_EMAIL,
                    VALID_PASSWORD_HASH, "Test", "09");
            buyer.setActive(true);
            when(userRepo.findByUsername(VALID_USERNAME)).thenReturn(Optional.of(buyer));

            // Nếu AuthService gọi UserSession → sẽ throw NPE trong test env (không có FX)
            // Test pass = AuthService không gọi UserSession
            assertDoesNotThrow(() -> authService.login(VALID_USERNAME, VALID_PASSWORD));
        }
    }

    // =========================================================================
    // register() tests
    // =========================================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký BUYER hợp lệ → tạo Buyer, gọi save()")
        void register_validBuyer_createsAndSavesBuyer() throws Exception {
            // ARRANGE — username và email chưa tồn tại
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(false);

            // ACT
            User result = authService.register(
                    VALID_USERNAME, VALID_EMAIL, VALID_PASSWORD,
                    "Nguyen Van A", "0900000000", "BUYER");

            // ASSERT — phải trả về Buyer
            assertInstanceOf(Buyer.class, result,
                    "Role BUYER phải tạo ra instance của Buyer");
            assertEquals(VALID_USERNAME, result.getUsername());
            assertEquals(VALID_EMAIL, result.getEmail());

            // Password phải được hash, không được lưu plain text
            assertNotEquals(VALID_PASSWORD, result.getPasswordHash(),
                    "Password KHÔNG được lưu dưới dạng plain text");
            assertTrue(PasswordHasher.verify(VALID_PASSWORD, result.getPasswordHash()),
                    "Hash phải verify được với password gốc");

            // userRepo.save() phải được gọi đúng 1 lần
            verify(userRepo, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Đăng ký SELLER hợp lệ → tạo Seller")
        void register_validSeller_createsSeller() throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(false);

            User result = authService.register(
                    VALID_USERNAME, VALID_EMAIL, VALID_PASSWORD,
                    "Tran Thi B", "0911111111", "SELLER");

            assertInstanceOf(Seller.class, result);
        }

        @Test
        @DisplayName("Username đã tồn tại → DuplicateUserException (USERNAME)")
        void register_duplicateUsername_throwsDuplicateException() throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(true);

            DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                    () -> authService.register(VALID_USERNAME, VALID_EMAIL,
                            VALID_PASSWORD, "Test", "09", "BUYER"));

            assertEquals(DuplicateUserException.Field.USERNAME, ex.getDuplicateField());
            assertEquals(VALID_USERNAME, ex.getValue());

            // Email check không được gọi sau khi username fail (short-circuit)
            verify(userRepo, never()).existsByEmail(any());
            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("Email đã tồn tại → DuplicateUserException (EMAIL)")
        void register_duplicateEmail_throwsDuplicateException() throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(true);

            DuplicateUserException ex = assertThrows(DuplicateUserException.class,
                    () -> authService.register(VALID_USERNAME, VALID_EMAIL,
                            VALID_PASSWORD, "Test", "09", "BUYER"));

            assertEquals(DuplicateUserException.Field.EMAIL, ex.getDuplicateField());
            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("ArgumentCaptor: hash được lưu vào DB phải verify được")
        void register_savedUser_hasCorrectHashInRepo() throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(false);

            // Capture chính xác User object được truyền vào save()
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            authService.register(VALID_USERNAME, VALID_EMAIL, VALID_PASSWORD,
                    "Test", "09", "BUYER");

            verify(userRepo).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            // Kiểm tra hash trong DB hợp lệ
            assertTrue(PasswordHasher.verify(VALID_PASSWORD, savedUser.getPasswordHash()),
                    "Hash được lưu phải xác thực được với password gốc");
            // Kiểm tra plain text không bị lưu nhầm
            assertNotEquals(VALID_PASSWORD, savedUser.getPasswordHash());
        }
    }

    // =========================================================================
    // Validation tests — dùng @ParameterizedTest
    // =========================================================================

    @Nested
    @DisplayName("validateRegistration() — Username")
    class UsernameValidationTests {

        // @ParameterizedTest + @ValueSource: chạy test này với từng giá trị trong mảng
        // Thay cho việc viết 4 test method riêng biệt
        @ParameterizedTest(name = "username=''{0}'' → IllegalArgumentException")
        @ValueSource(strings = { "ab", "a", "" })  // quá ngắn (< 3 ký tự)
        @DisplayName("Username quá ngắn → IllegalArgumentException")
        void register_usernameTooShort_throws(String shortUsername) throws Exception {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(shortUsername, VALID_EMAIL,
                            VALID_PASSWORD, "Test", "09", "BUYER"));

            // DB không được chạm đến nếu validation fail sớm
            verify(userRepo, never()).existsByUsername(any());
        }

        @ParameterizedTest(name = "username=''{0}'' có ký tự đặc biệt → IllegalArgumentException")
        @ValueSource(strings = { "user name", "user@123", "tên.người", "user!" })
        @DisplayName("Username có ký tự không hợp lệ → IllegalArgumentException")
        void register_usernameWithSpecialChars_throws(String invalidUsername) {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(invalidUsername, VALID_EMAIL,
                            VALID_PASSWORD, "Test", "09", "BUYER"));
        }

        @ParameterizedTest(name = "username=''{0}'' hợp lệ → không throw")
        @ValueSource(strings = { "abc", "user_123", "NGUYEN_VAN_A", "test99" })
        @DisplayName("Username hợp lệ → không throw exception")
        void register_validUsername_doesNotThrow(String validUsername) throws Exception {
            when(userRepo.existsByUsername(validUsername)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(false);

            assertDoesNotThrow(() -> authService.register(validUsername, VALID_EMAIL,
                    VALID_PASSWORD, "Test", "09", "BUYER"));
        }
    }

    @Nested
    @DisplayName("validateRegistration() — Email")
    class EmailValidationTests {

        @ParameterizedTest(name = "email=''{0}'' không hợp lệ → IllegalArgumentException")
        @ValueSource(strings = {
                "notanemail",
                "missing@dot",
                "@nodomain.com",
                "no-at-sign.com",
                ""
        })
        @DisplayName("Email không đúng format → IllegalArgumentException")
        void register_invalidEmail_throws(String invalidEmail) {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(VALID_USERNAME, invalidEmail,
                            VALID_PASSWORD, "Test", "09", "BUYER"));
        }

        @ParameterizedTest(name = "email=''{0}'' hợp lệ → không throw")
        @ValueSource(strings = {
                "user@example.com",
                "user.name+tag@domain.co",
                "test123@gmail.com"
        })
        @DisplayName("Email đúng format → không throw exception")
        void register_validEmail_doesNotThrow(String validEmail) throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(validEmail)).thenReturn(false);

            assertDoesNotThrow(() -> authService.register(VALID_USERNAME, validEmail,
                    VALID_PASSWORD, "Test", "09", "BUYER"));
        }
    }

    @Nested
    @DisplayName("validateRegistration() — Password strength")
    class PasswordStrengthTests {

        @ParameterizedTest(name = "password=''{0}'' yếu → IllegalArgumentException")
        @ValueSource(strings = {
                "short1A",       // < 8 ký tự
                "alllowercase1", // không có chữ hoa
                "ALLUPPERCASE1", // không có chữ thường
                "NoDigitsHere",  // không có số
                "12345678"       // chỉ có số
        })
        @DisplayName("Password yếu → IllegalArgumentException")
        void register_weakPassword_throws(String weakPassword) {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register(VALID_USERNAME, VALID_EMAIL,
                            weakPassword, "Test", "09", "BUYER"));
        }

        @ParameterizedTest(name = "password=''{0}'' đủ mạnh → không throw")
        @ValueSource(strings = {
                "Password1",
                "Secure99Pass",
                "Abc12345",
                "Test1234"
        })
        @DisplayName("Password đủ mạnh → không throw exception")
        void register_strongPassword_doesNotThrow(String strongPassword) throws Exception {
            when(userRepo.existsByUsername(VALID_USERNAME)).thenReturn(false);
            when(userRepo.existsByEmail(VALID_EMAIL)).thenReturn(false);

            assertDoesNotThrow(() -> authService.register(VALID_USERNAME, VALID_EMAIL,
                    strongPassword, "Test", "09", "BUYER"));
        }
    }

    // =========================================================================
    // PasswordHasher unit tests (static utility, không cần mock)
    // =========================================================================

    @Nested
    @DisplayName("PasswordHasher (static utility)")
    class PasswordHasherTests {

        @Test
        @DisplayName("hash() trả về chuỗi BCrypt hợp lệ (bắt đầu bằng $2a$)")
        void hash_returnsValidBcryptString() {
            String hash = PasswordHasher.hash("Test1234");
            assertTrue(hash.startsWith("$2a$"),
                    "BCrypt hash phải bắt đầu bằng $2a$");
            assertEquals(60, hash.length(),
                    "BCrypt hash luôn dài 60 ký tự");
        }

        @Test
        @DisplayName("Cùng password → 2 lần hash cho ra kết quả KHÁC nhau (salt ngẫu nhiên)")
        void hash_samePassword_differentHashEachTime() {
            String hash1 = PasswordHasher.hash("Test1234");
            String hash2 = PasswordHasher.hash("Test1234");
            assertNotEquals(hash1, hash2,
                    "BCrypt phải tạo salt ngẫu nhiên mỗi lần — hash không được giống nhau");
        }

        @Test
        @DisplayName("verify() đúng password → true")
        void verify_correctPassword_returnsTrue() {
            String hash = PasswordHasher.hash("Test1234");
            assertTrue(PasswordHasher.verify("Test1234", hash));
        }

        @Test
        @DisplayName("verify() sai password → false")
        void verify_wrongPassword_returnsFalse() {
            String hash = PasswordHasher.hash("Test1234");
            assertFalse(PasswordHasher.verify("WrongPass", hash));
        }

        @Test
        @DisplayName("verify() null input → false (không crash)")
        void verify_nullInput_returnsFalse() {
            assertFalse(PasswordHasher.verify(null, "$2a$12$somehash"));
            assertFalse(PasswordHasher.verify("password", null));
        }

        @Test
        @DisplayName("isStrong() — password đủ mạnh → true")
        void isStrong_strongPassword_returnsTrue() {
            assertTrue(PasswordHasher.isStrong("Password1"));
            assertTrue(PasswordHasher.isStrong("Abc12345"));
        }

        @Test
        @DisplayName("isStrong() — password yếu → false")
        void isStrong_weakPassword_returnsFalse() {
            assertFalse(PasswordHasher.isStrong(null));
            assertFalse(PasswordHasher.isStrong("short1A")); // < 8 ký tự
            assertFalse(PasswordHasher.isStrong("allowercase1")); // không hoa
            assertFalse(PasswordHasher.isStrong("NOLOWERCASE1")); // không thường
            assertFalse(PasswordHasher.isStrong("NoDigitsHere")); // không số
        }
    }

    // =========================================================================
    // logout() tests
    // =========================================================================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("logout() không throw exception với username hợp lệ")
        void logout_validUsername_doesNotThrow() {
            // logout() hiện tại chỉ ghi log — đảm bảo không crash
            assertDoesNotThrow(() -> authService.logout(VALID_USERNAME));
        }

        @Test
        @DisplayName("logout() không gọi DB (pure log operation)")
        void logout_doesNotCallRepository() {
            authService.logout(VALID_USERNAME);
            // Đảm bảo không có side effect nào với DB
            verifyNoInteractions(userRepo);
        }
    }
}
