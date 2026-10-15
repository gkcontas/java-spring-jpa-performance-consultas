package com.gkcontas.queryperf.catalog.repository;

import com.gkcontas.queryperf.catalog.Publisher;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    /**
     * Cached query over a cached entity, which is the only combination in which the
     * query cache helps: the cache holds the ids, and the ids resolve against the
     * entity cache without touching the database.
     */
    @QueryHints(@jakarta.persistence.QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select p from Publisher p where p.country = :country order by p.id")
    List<Publisher> findByCountryCached(@Param("country") String country);
}
