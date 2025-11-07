//package org.veto.core.rdbms.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.veto.core.rdbms.bean.UserFlowLimit;
//import org.veto.shared.COIN_TYPE;
//
//public interface UserFlowLimitRepository extends JpaRepository<UserFlowLimit, Long>, JpaSpecificationExecutor<UserFlowLimit> {
//    UserFlowLimit findByUserIdAndCoinType(Long userId, COIN_TYPE coinType);
//}
