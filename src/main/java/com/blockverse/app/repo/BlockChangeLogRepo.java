package com.blockverse.app.repo;

import com.blockverse.app.entity.BlockChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockChangeLogRepo extends JpaRepository<BlockChangeLog, Integer> {
}
