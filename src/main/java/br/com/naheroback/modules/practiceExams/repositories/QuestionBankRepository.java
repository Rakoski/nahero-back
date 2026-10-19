package br.com.naheroback.modules.practiceExams.repositories;

import br.com.naheroback.modules.enrollment.entities.QEnrollment;
import br.com.naheroback.modules.practiceExams.entities.QDomain;
import br.com.naheroback.modules.practiceExams.entities.QPracticeExam;
import br.com.naheroback.modules.practiceExams.entities.QQuestion;
import br.com.naheroback.modules.practiceExams.entities.QQuestionStudyAnswer;
import br.com.naheroback.modules.practiceExams.entities.QQuestionType;
import br.com.naheroback.modules.practiceExams.entities.QStudentAnswer;
import br.com.naheroback.modules.practiceExams.entities.QStudentPracticeAttempt;
import br.com.naheroback.modules.practiceExams.entities.Question;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionBankStatus;
import br.com.naheroback.modules.practiceExams.entities.enums.QuestionTypeEnum;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class QuestionBankRepository {

    private static final List<Integer> CHOICE_TYPE_IDS = List.of(
            QuestionTypeEnum.MULTIPLE_CHOICE.getId(),
            QuestionTypeEnum.TRUE_FALSE.getId(),
            QuestionTypeEnum.OBJECTIVE.getId());

    private static final QQuestion question = QQuestion.question;
    private static final QPracticeExam practiceExam = QPracticeExam.practiceExam;
    private static final QQuestionType questionType = QQuestionType.questionType;
    private static final QDomain domain = QDomain.domain;

    private final EntityManager entityManager;

    public record Filter(Integer studentId,
                         String language,
                         Integer practiceExamId,
                         Integer domainId,
                         QuestionBankStatus status) {}

    public List<Question> findPage(Filter filter, long offset, int limit) {
        return visibleQuestions()
                .where(matching(filter))
                .orderBy(question.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<Integer> findIds(Filter filter, int limit) {
        return new JPAQueryFactory(entityManager)
                .select(question.id)
                .from(question)
                .join(question.practiceExam, practiceExam)
                .join(question.questionType, questionType)
                .where(visible(), matching(filter))
                .orderBy(question.id.asc())
                .limit(limit)
                .fetch();
    }

    public Optional<Question> findVisible(Integer questionId) {
        return Optional.ofNullable(visibleQuestions()
                .where(question.id.eq(questionId))
                .fetchOne());
    }

    public Set<Integer> findVisibleIds(Collection<Integer> questionIds) {
        if (questionIds.isEmpty()) return Set.of();

        return new HashSet<>(new JPAQueryFactory(entityManager)
                .select(question.id)
                .from(question)
                .join(question.practiceExam, practiceExam)
                .join(question.questionType, questionType)
                .where(visible(), question.id.in(questionIds))
                .fetch());
    }

    public List<Question> findVisibleByIds(Collection<Integer> questionIds) {
        if (questionIds.isEmpty()) return List.of();

        return visibleQuestions()
                .where(question.id.in(questionIds))
                .fetch();
    }

    public Set<Integer> findAnsweredAmong(Integer studentId, Collection<Integer> questionIds) {
        if (questionIds.isEmpty()) return Set.of();

        return new HashSet<>(new JPAQueryFactory(entityManager)
                .select(question.id)
                .from(question)
                .where(question.id.in(questionIds), answeredBy(studentId))
                .fetch());
    }

    private JPAQuery<Question> visibleQuestions() {
        return new JPAQueryFactory(entityManager)
                .selectFrom(question)
                .join(question.practiceExam, practiceExam).fetchJoin()
                .join(question.questionType, questionType).fetchJoin()
                .leftJoin(question.domain, domain).fetchJoin()
                .where(visible());
    }

    private BooleanExpression visible() {
        return question.isActive.isNull().or(question.isActive.isTrue())
                .and(practiceExam.isActive.isNull().or(practiceExam.isActive.isTrue()))
                .and(questionType.id.in(CHOICE_TYPE_IDS));
    }

    private BooleanBuilder matching(Filter filter) {
        BooleanBuilder where = new BooleanBuilder(question.language.eq(filter.language()));

        if (filter.practiceExamId() != null) where.and(practiceExam.id.eq(filter.practiceExamId()));
        if (filter.domainId() != null) where.and(question.domain.id.eq(filter.domainId()));
        if (filter.status() == QuestionBankStatus.RESOLVED) where.and(answeredBy(filter.studentId()));
        if (filter.status() == QuestionBankStatus.UNRESOLVED) where.and(answeredBy(filter.studentId()).not());

        return where;
    }

    private BooleanExpression answeredBy(Integer studentId) {
        QStudentAnswer studentAnswer = QStudentAnswer.studentAnswer;
        QStudentPracticeAttempt attempt = QStudentPracticeAttempt.studentPracticeAttempt;
        QEnrollment enrollment = QEnrollment.enrollment;
        QQuestionStudyAnswer studyAnswer = QQuestionStudyAnswer.questionStudyAnswer;

        BooleanExpression answeredInAttempt = JPAExpressions.selectOne()
                .from(studentAnswer)
                .join(studentAnswer.studentPracticeAttempt, attempt)
                .join(attempt.enrollment, enrollment)
                .where(studentAnswer.questionId.eq(question.id),
                        studentAnswer.selectedAlternativeId.isNotNull(),
                        enrollment.student.id.eq(studentId))
                .exists();

        BooleanExpression answeredInStudy = JPAExpressions.selectOne()
                .from(studyAnswer)
                .where(studyAnswer.questionId.eq(question.id),
                        studyAnswer.studentId.eq(studentId))
                .exists();

        return answeredInAttempt.or(answeredInStudy);
    }
}
