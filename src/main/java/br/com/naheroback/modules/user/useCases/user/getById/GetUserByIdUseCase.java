package br.com.naheroback.modules.user.useCases.user.getById;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUserByIdUseCase {
    private final UserRepository userRepository;
    private final GetUserByIdResponse getUserByIdResponse;

    public GetUserByIdResponse execute(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.with(User.class, "id", id.toString()));

        return getUserByIdResponse.toPresentation(user);
    }
}
