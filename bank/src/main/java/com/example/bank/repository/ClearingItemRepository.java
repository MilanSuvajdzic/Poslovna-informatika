package com.example.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bank.model.ClearingItem;



@Repository
public interface ClearingItemRepository extends JpaRepository<ClearingItem, Long> {

}
