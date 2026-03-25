package com.dawayo.packing.Repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dawayo.packing.VO.PackingVO;
import com.dawayo.packing.VO.ScanErrorVO;
import com.dawayo.packing.VO.UserVO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
@Repository
public class UserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    public UserVO login(UserVO userVO) {
    String userId = userVO.getUserid();
    String password = userVO.getPassword();

    try {
        return entityManager.createQuery(
            "SELECT u FROM UserVO u WHERE u.userid = :userId AND u.password = :password", UserVO.class)
            .setParameter("userId", userId)
            .setParameter("password", password)
            .getSingleResult(); 
    } catch (jakarta.persistence.NoResultException e) {
        // 결과가 없을 경우 안전하게 null 반환 또는 예외 처리
        System.err.println("로그인 실패: 아이디나 비밀번호가 틀립니다. (" + userId + ")");
        return null;
    }
}

    public List<PackingVO> getPackingList(String sessionId) {
      return entityManager.createNativeQuery(
    "SELECT * FROM packing_list p " +
    "WHERE p.packer_id = :sessionId " +
    "AND p.id IN (SELECT MIN(id) FROM packing_list WHERE packer_id = :sessionId GROUP BY order_number)",
    PackingVO.class
).setParameter("sessionId", sessionId)
 .getResultList();

    }



    
}
