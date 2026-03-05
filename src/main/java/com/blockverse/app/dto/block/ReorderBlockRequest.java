package com.blockverse.app.dto.block;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class ReorderBlockRequest {
    private BigInteger newPosition;
}
