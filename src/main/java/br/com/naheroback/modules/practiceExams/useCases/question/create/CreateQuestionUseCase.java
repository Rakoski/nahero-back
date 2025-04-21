package br.com.naheroback.modules.practiceExams.useCases.question.create;

import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreateQuestionUseCase {
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public List<Question> execute(List<CreateQuestionRequest> requests) {
        int teacherId = AuthService.getUserFromToken().getId();
        List<Question> savedQuestions = new ArrayList<>();

        for (CreateQuestionRequest request : requests) {
            int version = 0;

            if (Objects.nonNull(request.baseQuestionId())) {
                version = questionRepository.findVersionByBaseQuestionId(request.baseQuestionId());
            }

            Question question = CreateQuestionRequest.toDomain(request, teacherId, version + 1);
            Question savedQuestion = questionRepository.save(question);

            List<Alternative> alternatives = CreateQuestionRequest.alternativesToDomain(request, savedQuestion, version + 1);
            if (!alternatives.isEmpty()) {
                alternativeRepository.saveAll(alternatives);
            }

            savedQuestions.add(savedQuestion);
        }

        return savedQuestions;
    }
}