package br.com.naheroback.modules.practiceExams.useCases.question.create;

import br.com.naheroback.common.exceptions.custom.BadRequestException;
import br.com.naheroback.modules.auth.services.AuthService;
import br.com.naheroback.modules.practiceExams.entities.Alternative;
import br.com.naheroback.modules.practiceExams.entities.PracticeExam;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.repositories.AlternativeRepository;
import br.com.naheroback.modules.practiceExams.repositories.PracticeExamRepository;
import br.com.naheroback.modules.practiceExams.repositories.QuestionRepository;
import br.com.naheroback.modules.practiceExams.services.ExamDomains;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateQuestionUseCase {
    private final QuestionRepository questionRepository;
    private final AlternativeRepository alternativeRepository;
    private final PracticeExamRepository practiceExamRepository;
    private final ExamDomains examDomains;

    @Transactional
    @Secured("IS_TEACHER")
    public List<Question> execute(List<CreateQuestionRequest> requests) {
        int teacherId = AuthService.getUserFromToken().getId();
        List<String> domains = resolveDomains(requests);
        List<Question> savedQuestions = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateQuestionRequest request = requests.get(i);
            int version = 0;

            if (Objects.nonNull(request.baseQuestionId())) version = questionRepository.findVersionByBaseQuestionId(request.baseQuestionId());

            Question question = CreateQuestionRequest.toDomain(request, teacherId, version + 1);
            question.setDomain(domains.get(i));
            Question savedQuestion = questionRepository.save(question);

            List<Alternative> alternatives = CreateQuestionRequest.alternativesToDomain(request, savedQuestion, version + 1);
            if (!alternatives.isEmpty()) alternativeRepository.saveAll(alternatives);

            savedQuestions.add(savedQuestion);
        }

        return savedQuestions;
    }

    private List<String> resolveDomains(List<CreateQuestionRequest> requests) {
        List<Integer> examIds = requests.stream()
                .filter(request -> hasDomain(request) && request.practiceExamId() != null)
                .map(CreateQuestionRequest::practiceExamId)
                .distinct()
                .toList();

        Map<Integer, String> slugByExamId = examIds.isEmpty()
                ? Map.of()
                : practiceExamRepository.findAllByIdIn(examIds).stream()
                        .collect(Collectors.toMap(PracticeExam::getId, PracticeExam::getSlug));

        List<String> resolved = new ArrayList<>();
        List<String> rejections = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateQuestionRequest request = requests.get(i);

            if (!hasDomain(request)) {
                resolved.add(null);
                continue;
            }

            String slug = slugByExamId.get(request.practiceExamId());
            if (slug == null) {
                rejections.add("index " + i + ": unknown practiceExamId " + request.practiceExamId());
                resolved.add(null);
                continue;
            }
            if (!examDomains.hasPracticeExam(slug)) {
                rejections.add("index " + i + ": no domain map for practice exam '" + slug + "'");
                resolved.add(null);
                continue;
            }

            Optional<String> canonical = examDomains.canonicalName(slug, request.domain());
            if (canonical.isEmpty()) {
                rejections.add("index " + i + ": '" + request.domain()
                        + "' is not a domain of '" + slug + "'");
                resolved.add(null);
                continue;
            }

            resolved.add(canonical.get());
        }

        if (!rejections.isEmpty()) {
            throw new BadRequestException("Nothing was written. " + String.join("; ", rejections));
        }

        return resolved;
    }

    private boolean hasDomain(CreateQuestionRequest request) {
        return request.domain() != null && !request.domain().isBlank();
    }
}
