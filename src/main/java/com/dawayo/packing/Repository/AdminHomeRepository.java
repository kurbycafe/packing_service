package com.dawayo.packing.Repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Transactional
@Repository
public class AdminHomeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    //count packed items on today group by orderNumber
    public Long countPackedItemsToday() {
        String today = java.time.LocalDate.now().toString();

        Long count = entityManager.createQuery(
                "SELECT COUNT(p) FROM PackingVO p WHERE p.packingDate = :today", Long.class)
                .setParameter("today", today)
                .getSingleResult();
        return count;
    }
    public Long countPackedItemsThisWeek() {
        String startOfWeek = java.time.LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY)
                .toString();
        String endOfWeek = java.time.LocalDate.now()
                .with(java.time.DayOfWeek.SUNDAY)
                .toString();

        Long count = entityManager.createQuery(
                "SELECT COUNT(p) FROM PackingVO p WHERE p.packingDate BETWEEN :startOfWeek AND :endOfWeek", Long.class)
                .setParameter("startOfWeek", startOfWeek)
                .setParameter("endOfWeek", endOfWeek)
                .getSingleResult();
        return count;
    }

    public Long countPackedItemsThisMonth() {
        String startOfMonth = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
                .toString();
        String endOfMonth = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
                .toString();

        Long count = entityManager.createQuery(
                "SELECT COUNT(p) FROM PackingVO p WHERE p.packingDate BETWEEN :startOfMonth AND :endOfMonth", Long.class)
                .setParameter("startOfMonth", startOfMonth)
                .setParameter("endOfMonth", endOfMonth)
                .getSingleResult();
        return count;
    }
}
