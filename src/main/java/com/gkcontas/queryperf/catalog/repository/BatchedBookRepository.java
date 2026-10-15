package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.BatchedBook;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BatchedBookRepository extends JpaRepository<BatchedBook, Long> {

    @Query("select b from BatchedBook b order by b.id")
    List<BatchedBook> findAllPlain();

    @Query(value = "select b from BatchedBook b order by b.id",
            countQuery = "select count(b) from BatchedBook b")
    Page<BatchedBook> findPagePlain(Pageable pageable);
}
