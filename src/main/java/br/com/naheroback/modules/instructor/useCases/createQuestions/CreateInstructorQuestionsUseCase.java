package br.com.naheroback.modules.instructor.useCases.createQuestions;

import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.common.utils.ValidateNull;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.instructor.services.InstructorAccess;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.Domain;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.PracticeExamDomain;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.QuestionType;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamDomainRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateInstructorQuestionsUseCase {
    private static final Set<QuestionTypeEnum> SUPPORTED_TYPES =
            Set.of(QuestionTypeEnum.MULTIPLE_CHOICE, QuestionTypeEnum.TRUE_FALSE, QuestionTypeEnum.OBJECTIVE);

    private final InstructorAccess instructorAccess;
    private final PracticeExamDomainRepository practiceExamDomainRepository;
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;

    @Transactional
    @Secured("IS_TEACHER")
    public CreateInstructorQuestionsResponse execute(Integer practiceExamId, CreateInstructorQuestionsRequest request) {
        PracticeExam practiceExam = instructorAccess.ownedPracticeExam(practiceExamId);
        User teacher = AuthService.getUserFromToken();
        Map<Integer, Domain> domains = practiceExamDomainRepository.findAllByPracticeExamIdIn(List.of(practiceExamId))
                .stream()
                .map(PracticeExamDomain::getDomain)
                .collect(Collectors.toMap(Domain::getId, domain -> domain));

        for (int i = 0; i < request.questions().size(); i++) {
            validate(i + 1, request.questions().get(i), domains);
        }

        List<Integer> questionIds = new ArrayList<>();
        for (CreateInstructorQuestionsRequest.QuestionRequest questionRequest : request.questions()) {
            Question question = questionRepository.save(
                    toQuestion(questionRequest, practiceExam, teacher, request.language(), domains));
            alternativeRepository.saveAll(questionRequest.alternatives().stream()
                    .map(alternativeRequest -> toAlternative(alternativeRequest, question, teacher))
                    .toList());
            questionIds.add(question.getId());
        }

        return new CreateInstructorQuestionsResponse(questionIds.size(), questionIds);
    }

    private void validate(int position, CreateInstructorQuestionsRequest.QuestionRequest request, Map<Integer, Domain> domains) {
        if (!SUPPORTED_TYPES.contains(request.type())) {
            throw new UnprocessableEntityException("instructor.question.type_unsupported", position);
        }
        if (!domains.containsKey(request.domainId())) {
            throw new UnprocessableEntityException("instructor.question.domain_not_linked", position);
        }

        long correct = request.alternatives().stream().filter(alternative -> alternative.isCorrect()).count();
        if (correct == 0 || correct == request.alternatives().size()) {
            throw new UnprocessableEntityException("instructor.question.needs_correct_and_incorrect", position);
        }
        if (request.type() == QuestionTypeEnum.TRUE_FALSE && request.alternatives().size() != 2) {
            throw new UnprocessableEntityException("instructor.question.true_false_size", position);
        }
        if (request.type() != QuestionTypeEnum.MULTIPLE_CHOICE && correct != 1) {
            throw new UnprocessableEntityException("instructor.question.single_answer", position);
        }
    }

    private Question toQuestion(CreateInstructorQuestionsRequest.QuestionRequest request, PracticeExam practiceExam,
                                User teacher, String language, Map<Integer, Domain> domains) {
        Question question = new Question();
        question.setPracticeExam(practiceExam);
        question.setQuestionType(ValidateNull.validate(QuestionType.class, request.type().getId()));
        question.setTeacher(teacher);
        question.setContent(request.content().trim());
        question.setExplanation(request.explanation() == null || request.explanation().isBlank()
                ? null
                : request.explanation().trim());
        question.setPoints(1);
        question.setVersion(1);
        question.setIsActive(true);
        question.setLanguage(language);
        question.setDomain(domains.get(request.domainId()));
        return question;
    }

    private Alternative toAlternative(CreateInstructorQuestionsRequest.AlternativeRequest request, Question question,
                                      User teacher) {
        Alternative alternative = new Alternative();
        alternative.setQuestion(question);
        alternative.setTeacher(teacher);
        alternative.setContent(request.content().trim());
        alternative.setIsCorrect(request.isCorrect());
        alternative.setVersion(1);
        alternative.setIsActive(true);
        return alternative;
    }
}
