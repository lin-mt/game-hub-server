package com.app.gamehub.model;

import com.app.gamehub.entity.WarArrangement;
import com.app.gamehub.entity.WarGroup;
import java.util.List;
import lombok.Data;

@Data
public class TacticalArrangement {

  private WarGroup warGroup;

  private List<WarArrangement> warArrangements;
}
