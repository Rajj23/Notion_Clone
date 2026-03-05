package com.blockverse.app.dto.block;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class MoveBlockRequest {
    private Integer newParentId;
    private BigInteger newPosition;
}
