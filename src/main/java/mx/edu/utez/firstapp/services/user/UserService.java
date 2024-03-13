package mx.edu.utez.firstapp.services.user;

import mx.edu.utez.firstapp.config.ApiResponse;
import mx.edu.utez.firstapp.models.person.Person;
import mx.edu.utez.firstapp.models.person.PersonRepository;
import mx.edu.utez.firstapp.models.role.Role;
import mx.edu.utez.firstapp.models.role.RoleRepository;
import mx.edu.utez.firstapp.models.user.User;
import mx.edu.utez.firstapp.models.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository repository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository repository, PersonRepository personRepository, RoleRepository roleRepository) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> findAll() {
        return new ResponseEntity<>(new ApiResponse(repository.findAll(), HttpStatus.OK), HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> findUserById(Long id) {
        return new ResponseEntity<>(new ApiResponse(repository.findById(id), HttpStatus.OK), HttpStatus.OK);
    }

    @Transactional(rollbackFor = {SQLException.class})
    public ResponseEntity<ApiResponse> save(User user, Long personId) {
        Optional<User> foundUser = repository.findByUsername(user.getUsername());
        if (foundUser.isPresent())
            return new ResponseEntity<>(new ApiResponse(HttpStatus.BAD_REQUEST, true, "RecordAlreadyExist"),
                    HttpStatus.BAD_REQUEST);
        Person person = personRepository.findById(personId).orElse(null);
        if (person == null)
            return new ResponseEntity<>(new ApiResponse(HttpStatus.BAD_REQUEST, true, "PersonNotFound"),
                    HttpStatus.BAD_REQUEST);
        user.setPerson(person);
        Set<Role> roles = user.getRoles();
        user.setRoles(null);
        repository.save(user);
        for (Role role : roles) {
            if (roleRepository.saveUserRole(role.getId(), user.getId()) <= 0)
                return new ResponseEntity<>(new ApiResponse(HttpStatus.BAD_REQUEST, true, "RoleNotAttached"),
                        HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new ApiResponse(repository.findById(user.getId()), HttpStatus.OK), HttpStatus.OK);
    }

}
