package br.com.naheroback.modules.exams.useCases.create;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.exams.entities.Exam;
import br.com.naheroback.modules.exams.repositories.ExamRepository;
import br.com.naheroback.modules.user.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateExamUseCaseTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CreateExamResponse createExamResponse;

    @InjectMocks
    private CreateExamUseCase createExamUseCase;

    private User mockUser;
    private Exam mockExam;
    private CreateExamRequest mockRequest;
    private CreateExamResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test Teacher");
        mockUser.setEmail("teacher@example.com");

        mockExam = new Exam();
        mockExam.setId(1);
        mockExam.setTitle("Math Exam");
        mockExam.setDescription("Test description");
        mockExam.setCategory("Mathematics");
        mockExam.setDifficultyLevel(3);
        mockExam.setIsActive(true);
        mockExam.setTeacher(mockUser);

        mockRequest = new CreateExamRequest(
                "Math Exam",
                "Test description",
                "Mathematics",
                3,
                true
        );

        mockResponse = new CreateExamResponse();
    }

    @Test
    @DisplayName("Should create exam successfully")
    void shouldCreateExamSuccessfully() {
        try (MockedStatic<AuthService> mockedStatic = mockStatic(AuthService.class)) {
            mockedStatic.when(AuthService::getUserFromToken).thenReturn(mockUser);
            
            Exam examToSave = mockRequest.toDomain(mockUser);

            when(examRepository.save(any(Exam.class))).thenReturn(mockExam);
            
            when(createExamResponse.toPresentation(mockExam)).thenReturn(mockResponse);
            
            CreateExamResponse result = createExamUseCase.execute(mockRequest);
            
            assertNotNull(result);
            assertEquals(mockResponse, result);
            
            verify(examRepository, times(1)).save(any(Exam.class));
            verify(createExamResponse, times(1)).toPresentation(mockExam);
        }
    }
}