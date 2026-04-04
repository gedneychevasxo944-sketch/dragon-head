//package org.dragon.api.controller;
//
//import org.dragon.character.Character;
//import org.dragon.character.CharacterRegistry;
//import org.dragon.character.profile.CharacterProfile;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
///**
// * CharacterController 单元测试
// */
//@ExtendWith(MockitoExtension.class)
//class CharacterControllerTest {
//
//    @Mock
//    private CharacterRegistry characterRegistry;
//
//    @InjectMocks
//    private CharacterController characterController;
//
//    private Character testCharacter;
//
//    @BeforeEach
//    void setUp() {
//        testCharacter = new Character();
//        CharacterProfile profile = new CharacterProfile();
//        profile.setId("char-1");
//        profile.setName("Test Character");
//        testCharacter.setProfile(profile);
//    }
//
//    @Test
//    void testListAll() {
//        when(characterRegistry.listAll()).thenReturn(List.of(testCharacter));
//
//        ResponseEntity<List<Character>> response = characterController.listAll();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(1, response.getBody().size());
//        verify(characterRegistry).listAll();
//    }
//
//    @Test
//    void testGetCharacter() {
//        when(characterRegistry.get("char-1")).thenReturn(Optional.of(testCharacter));
//
//        ResponseEntity<Character> response = characterController.getCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("char-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetCharacterNotFound() {
//        when(characterRegistry.get("nonexistent")).thenReturn(Optional.empty());
//
//        ResponseEntity<Character> response = characterController.getCharacter("nonexistent");
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetDefaultCharacter() {
//        when(characterRegistry.getDefaultCharacter()).thenReturn(Optional.of(testCharacter));
//
//        ResponseEntity<Character> response = characterController.getDefaultCharacter();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("char-1", response.getBody().getId());
//    }
//
//    @Test
//    void testGetDefaultCharacterNotSet() {
//        when(characterRegistry.getDefaultCharacter()).thenReturn(Optional.empty());
//
//        ResponseEntity<Character> response = characterController.getDefaultCharacter();
//
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testRegisterCharacter() {
//        doNothing().when(characterRegistry).register(any(Character.class));
//
//        ResponseEntity<Character> response = characterController.registerCharacter(testCharacter);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertEquals(testCharacter, response.getBody());
//        verify(characterRegistry).register(testCharacter);
//    }
//
//    @Test
//    void testUpdateCharacter() {
//        doNothing().when(characterRegistry).update(any(Character.class));
//
//        Character updateData = new Character();
//        CharacterProfile profile = new CharacterProfile();
//        profile.setName("Updated");
//        updateData.setProfile(profile);
//
//        ResponseEntity<Character> response = characterController.updateCharacter("char-1", updateData);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("char-1", response.getBody().getId());
//        verify(characterRegistry).update(any(Character.class));
//    }
//
//    @Test
//    void testUnregisterCharacter() {
//        doNothing().when(characterRegistry).unregister("char-1");
//
//        ResponseEntity<Void> response = characterController.unregisterCharacter("char-1");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        verify(characterRegistry).unregister("char-1");
//    }
//
//    @Test
//    void testLoadCharacter() {
//        doNothing().when(characterRegistry).load("char-1");
//
//        ResponseEntity<Void> response = characterController.loadCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(characterRegistry).load("char-1");
//    }
//
//    @Test
//    void testStartCharacter() {
//        doNothing().when(characterRegistry).start("char-1");
//
//        ResponseEntity<Void> response = characterController.startCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(characterRegistry).start("char-1");
//    }
//
//    @Test
//    void testPauseCharacter() {
//        doNothing().when(characterRegistry).pause("char-1");
//
//        ResponseEntity<Void> response = characterController.pauseCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(characterRegistry).pause("char-1");
//    }
//
//    @Test
//    void testDestroyCharacter() {
//        doNothing().when(characterRegistry).destroy("char-1");
//
//        ResponseEntity<Void> response = characterController.destroyCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(characterRegistry).destroy("char-1");
//    }
//
//    @Test
//    void testSetDefaultCharacter() {
//        doNothing().when(characterRegistry).setDefaultCharacter("char-1");
//
//        ResponseEntity<Void> response = characterController.setDefaultCharacter("char-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(characterRegistry).setDefaultCharacter("char-1");
//    }
//}
