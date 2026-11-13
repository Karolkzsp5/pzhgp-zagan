package com.pzhgp.backend.controller;

import com.pzhgp.backend.config.AuthCookieService;
import com.pzhgp.backend.dto.ContactUpdateRequest;
import com.pzhgp.backend.dto.PasswordChangeRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.*;
import com.pzhgp.backend.service.JwtService;
import com.pzhgp.backend.service.MailService;
import com.pzhgp.backend.support.MutableClock;
import com.pzhgp.backend.support.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static com.pzhgp.backend.support.CookieAuthTestSupport.cookieAuth;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DisplayName("Account Integration Tests")
class AccountIntegrationTest {

    private static final String PASSWORD = "StareHaslo1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BreederRepository breederRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private BoardMemberRepository boardMemberRepository;

    @Autowired
    private ForumCategoryRepository forumCategoryRepository;

    @Autowired
    private ForumThreadRepository forumThreadRepository;

    @Autowired
    private ForumPostRepository forumPostRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private PigeonFlightRepository pigeonFlightRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JsonMapper objectMapper;

    // Atrapa zamiast serwera pocztowego.
    @MockitoBean
    private MailService mailService;

    private Section section;
    private Breeder breeder;
    private Breeder otherBreeder;
    private String token;

    @BeforeEach
    void setUp() {
        // Limit prób zmiany hasła jest wspólny dla testów; po przesunięciu zegara o dobę wcześniejsze próby wygasają.
        clock.advance(Duration.ofDays(1));

        section = sectionRepository.save(new Section(null, "Sekcja Ustawień", 1));
        breeder = createBreeder("jan.kowalski@test.pl", "600100200", Role.MODERATOR);
        otherBreeder = createBreeder("inny.hodowca@test.pl", "600100300", Role.BREEDER);
        token = jwtService.generateToken(breeder);
    }

    private Breeder createBreeder(String email, String phoneNumber, Role role) {
        Breeder account = new Breeder();
        account.setEmail(email);
        account.setName("Jan");
        account.setSurname("Kowalski");
        account.setDateOfBirth(LocalDate.parse("1985-04-20"));
        account.setPhoneNumber(phoneNumber);
        account.setPostalCode("68-100");
        account.setCity("Żagań");
        account.setStreet("Słowackiego");
        account.setHouseNumber("10");
        account.setRole(role);
        account.setStatus(AccountStatus.ACTIVE);
        account.setSection(section);
        account.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return breederRepository.save(account);
    }

    private ResultActions updateContact(ContactUpdateRequest request) throws Exception {
        return mockMvc.perform(put("/api/account/contact")
                .with(cookieAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions changePassword(String currentPassword, String newPassword) throws Exception {
        return mockMvc.perform(put("/api/account/password")
                .with(cookieAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PasswordChangeRequest(currentPassword, newPassword))));
    }

    private Breeder reload(Breeder account) {
        return breederRepository.findById(account.getId()).orElseThrow();
    }

    // --- Profil -------------------------------------------------------------

    @Test
    @DisplayName("Should return the profile, account details and board functions of the logged-in user")
    void shouldReturnAccountDetails() throws Exception {
        BoardMember branchFunction = new BoardMember();
        branchFunction.setRole(BoardRole.SEKRETARZ);
        branchFunction.setBreeder(breeder);
        boardMemberRepository.save(branchFunction);

        BoardMember sectionFunction = new BoardMember();
        sectionFunction.setRole(BoardRole.PREZES);
        sectionFunction.setManagedSection(section);
        sectionFunction.setBreeder(breeder);
        boardMemberRepository.save(sectionFunction);

        mockMvc.perform(get("/api/account").with(cookieAuth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan"))
                .andExpect(jsonPath("$.surname").value("Kowalski"))
                .andExpect(jsonPath("$.dateOfBirth").value("1985-04-20"))
                .andExpect(jsonPath("$.email").value("jan.kowalski@test.pl"))
                .andExpect(jsonPath("$.phoneNumber").value("600100200"))
                .andExpect(jsonPath("$.postalCode").value("68-100"))
                .andExpect(jsonPath("$.city").value("Żagań"))
                .andExpect(jsonPath("$.street").value("Słowackiego"))
                .andExpect(jsonPath("$.houseNumber").value("10"))
                .andExpect(jsonPath("$.sectionName").value("Sekcja Ustawień"))
                .andExpect(jsonPath("$.role").value("MODERATOR"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.boardFunctions.length()").value(2))
                .andExpect(jsonPath("$.boardFunctions[?(@ == 'Sekretarz – Zarząd Oddziału')]").exists())
                .andExpect(jsonPath("$.boardFunctions[?(@ == 'Prezes – Zarząd Sekcji Sekcja Ustawień')]").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.sessionVersion").doesNotExist());
    }

    @Test
    @DisplayName("Should require logging in to read the account")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account/export")).andExpect(status().isUnauthorized());
    }

    // --- Dane kontaktowe ------------------------------------------------------

    @Test
    @DisplayName("Should update the phone number and address")
    void shouldUpdateContactDetails() throws Exception {
        updateContact(new ContactUpdateRequest("600999888", "68-200", "Szprotawa", "Długa", "5/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("600999888"))
                .andExpect(jsonPath("$.city").value("Szprotawa"));

        Breeder saved = reload(breeder);
        assertEquals("600999888", saved.getPhoneNumber());
        assertEquals("68-200", saved.getPostalCode());
        assertEquals("Szprotawa", saved.getCity());
        assertEquals("Długa", saved.getStreet());
        assertEquals("5/2", saved.getHouseNumber());
        assertEquals("Jan", saved.getName());
        assertEquals("jan.kowalski@test.pl", saved.getEmail());
    }

    @Test
    @DisplayName("Should keep the own phone number when only the address changes")
    void shouldAllowKeepingOwnPhoneNumber() throws Exception {
        updateContact(new ContactUpdateRequest("600100200", "68-100", "Żagań", "Nowa", "1"))
                .andExpect(status().isOk());

        assertEquals("Nowa", reload(breeder).getStreet());
    }

    @Test
    @DisplayName("Should reject a phone number used by another account")
    void shouldRejectPhoneNumberOfAnotherAccount() throws Exception {
        updateContact(new ContactUpdateRequest("600100300", "68-100", "Żagań", "Słowackiego", "10"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Użytkownik z podanym numerem telefonu już istnieje."));

        assertEquals("600100200", reload(breeder).getPhoneNumber());
    }

    @Test
    @DisplayName("Should apply the registration rules to contact details")
    void shouldValidateContactDetails() throws Exception {
        updateContact(new ContactUpdateRequest("60010020", "68-100", "Żagań", "Słowackiego", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Numer telefonu musi mieć dokładnie 9 cyfr."));

        updateContact(new ContactUpdateRequest("600100200", "68100", "Żagań", "Słowackiego", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Niepoprawny format kodu pocztowego (wymagany: XX-XXX)."));

        updateContact(new ContactUpdateRequest("600100200", "68-100", "Żagań", "Słowackiego", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4)."));
    }

    // --- Zmiana hasła ---------------------------------------------------------

    @Test
    @DisplayName("Should change the password, end all sessions and send a security e-mail")
    void shouldChangePassword() throws Exception {
        breeder.setPasswordResetTokenHash("a".repeat(64));
        breeder.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(600));
        breederRepository.save(breeder);

        changePassword(PASSWORD, "NoweHaslo1!")
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(startsWith(AuthCookieService.COOKIE_NAME + "=; Path=/; Max-Age=0"))));

        Breeder saved = reload(breeder);
        assertTrue(passwordEncoder.matches("NoweHaslo1!", saved.getPasswordHash()));
        assertNull(saved.getPasswordResetTokenHash());
        assertNull(saved.getPasswordResetTokenExpiresAt());
        verify(mailService).sendPasswordChangedEmail("jan.kowalski@test.pl", "Jan");

        // Token wydany przed zmianą hasła już nie działa.
        mockMvc.perform(get("/api/account").with(cookieAuth(token))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account").with(cookieAuth(jwtService.generateToken(saved)))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject a wrong current password and keep the session")
    void shouldRejectWrongCurrentPassword() throws Exception {
        changePassword("ZleHaslo1!", "NoweHaslo1!")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Obecne hasło jest nieprawidłowe."));

        assertTrue(passwordEncoder.matches(PASSWORD, reload(breeder).getPasswordHash()));
        verifyNoInteractions(mailService);
        mockMvc.perform(get("/api/account").with(cookieAuth(token))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject a new password that is the same as the current one")
    void shouldRejectUnchangedPassword() throws Exception {
        changePassword(PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Nowe hasło musi się różnić od obecnego."));
    }

    @Test
    @DisplayName("Should apply the registration rules to the new password")
    void shouldValidateNewPassword() throws Exception {
        changePassword(PASSWORD, "slabe")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hasło musi mieć min. 8 znaków, zawierać dużą i małą literę, cyfrę oraz znak specjalny."));

        changePassword("", "NoweHaslo1!")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Podaj obecne hasło."));
    }

    @Test
    @DisplayName("Should return 429 after too many password change attempts on one account")
    void shouldLimitPasswordChangeAttempts() throws Exception {
        for (int i = 0; i < 5; i++) {
            changePassword("ZleHaslo1!", "NoweHaslo1!").andExpect(status().isBadRequest());
        }

        changePassword(PASSWORD, "NoweHaslo1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Zbyt wiele prób zmiany hasła. Spróbuj ponownie później."));

        assertTrue(passwordEncoder.matches(PASSWORD, reload(breeder).getPasswordHash()));
    }

    @Test
    @DisplayName("Should require the CSRF token to change the password")
    void shouldRequireCsrfTokenToChangePassword() throws Exception {
        mockMvc.perform(put("/api/account/password")
                        .cookie(new jakarta.servlet.http.Cookie(AuthCookieService.COOKIE_NAME, token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordChangeRequest(PASSWORD, "NoweHaslo1!"))))
                .andExpect(status().isForbidden());

        assertTrue(passwordEncoder.matches(PASSWORD, reload(breeder).getPasswordHash()));
    }

    // --- Wylogowanie ze wszystkich urządzeń -----------------------------------

    @Test
    @DisplayName("Should end every session of the account and keep other accounts logged in")
    void shouldLogOutAllDevices() throws Exception {
        String secondDeviceToken = jwtService.generateToken(breeder);
        String otherAccountToken = jwtService.generateToken(otherBreeder);

        mockMvc.perform(post("/api/account/logout-all").with(cookieAuth(token)))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(startsWith(AuthCookieService.COOKIE_NAME + "=; Path=/; Max-Age=0"))));

        mockMvc.perform(get("/api/account").with(cookieAuth(token))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account").with(cookieAuth(secondDeviceToken))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account").with(cookieAuth(otherAccountToken))).andExpect(status().isOk());

        // Po ponownym zalogowaniu nowy token działa.
        mockMvc.perform(get("/api/account").with(cookieAuth(jwtService.generateToken(reload(breeder))))).andExpect(status().isOk());
    }

    // --- Eksport danych ---------------------------------------------------------

    @Test
    @DisplayName("Should export the account data and the content added by the user only")
    void shouldExportOwnData() throws Exception {
        ForumCategory category = new ForumCategory();
        category.setName("Loty i treningi");
        category.setAuthor(otherBreeder);
        forumCategoryRepository.save(category);

        ForumThread ownThread = new ForumThread();
        ownThread.setCategory(category);
        ownThread.setAuthor(breeder);
        ownThread.setTitle("Mój wątek");
        forumThreadRepository.save(ownThread);

        ForumThread otherThread = new ForumThread();
        otherThread.setCategory(category);
        otherThread.setAuthor(otherBreeder);
        otherThread.setTitle("Cudzy wątek");
        forumThreadRepository.save(otherThread);

        ForumPost ownPost = new ForumPost();
        ownPost.setThread(otherThread);
        ownPost.setAuthor(breeder);
        ownPost.setBody("<p>Moja odpowiedź</p>");
        forumPostRepository.save(ownPost);

        ForumPost otherPost = new ForumPost();
        otherPost.setThread(otherThread);
        otherPost.setAuthor(otherBreeder);
        otherPost.setBody("<p>Cudza odpowiedź</p>");
        forumPostRepository.save(otherPost);

        Announcement announcement = new Announcement();
        announcement.setTitle("Moje ogłoszenie");
        announcement.setContent("<p>Treść ogłoszenia</p>");
        announcement.setAuthor(breeder);
        announcementRepository.save(announcement);

        PigeonFlight flight = new PigeonFlight();
        flight.setOwner(breeder);
        flight.setName("Lot treningowy");
        flight.setRingNumber("PL-0369-24-1234");
        flight.setOriginalFileName("trening.gpx");
        flight.setTrackDistanceMeters(12_500);
        flight.setDurationSeconds(900);
        pigeonFlightRepository.save(flight);

        mockMvc.perform(get("/api/account/export").with(cookieAuth(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.exportedAt").exists())
                .andExpect(jsonPath("$.account.email").value("jan.kowalski@test.pl"))
                .andExpect(jsonPath("$.account.phoneNumber").value("600100200"))
                .andExpect(jsonPath("$.forumThreads.length()").value(1))
                .andExpect(jsonPath("$.forumThreads[0].title").value("Mój wątek"))
                .andExpect(jsonPath("$.forumThreads[0].category").value("Loty i treningi"))
                .andExpect(jsonPath("$.forumPosts.length()").value(1))
                .andExpect(jsonPath("$.forumPosts[0].thread").value("Cudzy wątek"))
                .andExpect(jsonPath("$.forumPosts[0].body").value("<p>Moja odpowiedź</p>"))
                .andExpect(jsonPath("$.announcements.length()").value(1))
                .andExpect(jsonPath("$.announcements[0].title").value("Moje ogłoszenie"))
                .andExpect(jsonPath("$.flights.length()").value(1))
                .andExpect(jsonPath("$.flights[0].name").value("Lot treningowy"))
                .andExpect(jsonPath("$.flights[0].fileName").value("trening.gpx"))
                .andExpect(jsonPath("$.flights[0].trackDistanceMeters").value(12_500.0))
                .andExpect(jsonPath("$..passwordHash").isEmpty());
    }

    @Test
    @DisplayName("Should export empty lists for an account without any content")
    void shouldExportEmptyListsWithoutContent() throws Exception {
        mockMvc.perform(get("/api/account/export").with(cookieAuth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forumThreads.length()").value(0))
                .andExpect(jsonPath("$.forumPosts.length()").value(0))
                .andExpect(jsonPath("$.announcements.length()").value(0))
                .andExpect(jsonPath("$.flights.length()").value(0));

        verify(mailService, never()).sendPasswordChangedEmail(any(), any());
    }
}