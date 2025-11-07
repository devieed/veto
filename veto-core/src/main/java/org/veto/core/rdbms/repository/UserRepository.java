package org.veto.core.rdbms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    User findByUsernameAndPassword(String username, String password);

    boolean existsByRecommendCode(String recommendCode);

    boolean existsByRecommendCodeAndStatusIsTrue(String recommendCode);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    User findByRecommendCode(String recommendCode);

    Page<User> findAllByUsernameContains(String username, Pageable pageable);

    Page<User> findAllByIdIn(List<Long> ids, Pageable pageable);
}
