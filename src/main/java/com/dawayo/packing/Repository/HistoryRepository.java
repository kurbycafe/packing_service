package com.dawayo.packing.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dawayo.packing.VO.PackingVO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository

@Transactional
public class HistoryRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    public Page<PackingVO> findPackingList(Pageable pageable) {  // @Override 제거
        String jpql =
            "SELECT p FROM PackingVO p " +
            "WHERE p.id = (" +
            "   SELECT MAX(p2.id) FROM PackingVO p2 WHERE p2.orderNumber = p.orderNumber" +
            ") " +
            "ORDER BY p.orderNumber DESC";

        String countJpql =
            "SELECT COUNT(p) FROM PackingVO p " +
            "WHERE p.id = (" +
            "   SELECT MAX(p2.id) FROM PackingVO p2 WHERE p2.orderNumber = p.orderNumber" +
            ")";

        Long total = entityManager.createQuery(countJpql, Long.class)
                                .getSingleResult();

        List<PackingVO> content = entityManager
                .createQuery(jpql, PackingVO.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    public List<PackingVO> getPackingById(String id) {
       String jpql = "SELECT p FROM PackingVO p WHERE p.orderNumber = :id";
        return entityManager.createQuery(jpql, PackingVO.class)
        .setParameter("id", id)
        .getResultList();
     }



}
