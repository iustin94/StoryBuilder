package net.questforge.engine;

import net.questforge.model.Condition;
import net.questforge.model.PlayerQuestData;

import java.util.Objects;

public class ConditionEvaluator {

    public boolean evaluate(Condition cond, PlayerQuestData.QuestProgress progress) {
        Object val = progress.getVariables().get(cond.getVar());
        Object target = cond.getValue();
        switch (cond.getOp()) {
            case "==": return Objects.equals(val, coerce(target, val));
            case "!=": return !Objects.equals(val, coerce(target, val));
            case ">":  return toDouble(val) > toDouble(target);
            case ">=": return toDouble(val) >= toDouble(target);
            case "<":  return toDouble(val) < toDouble(target);
            case "<=": return toDouble(val) <= toDouble(target);
            default:   return false;
        }
    }

    private Object coerce(Object target, Object existing) {
        if (target == null || existing == null) {
            return target;
        }
        if (existing instanceof Boolean) {
            if (target instanceof Boolean) {
                return target;
            }
            if (target instanceof String) {
                return Boolean.parseBoolean((String) target);
            }
            if (target instanceof Number) {
                return ((Number) target).doubleValue() != 0;
            }
            return target;
        }
        if (existing instanceof Number) {
            if (target instanceof Number) {
                return target;
            }
            if (target instanceof String) {
                try {
                    return Double.parseDouble((String) target);
                } catch (NumberFormatException e) {
                    return target;
                }
            }
            if (target instanceof Boolean) {
                return ((Boolean) target) ? 1.0 : 0.0;
            }
            return target;
        }
        if (existing instanceof String) {
            return String.valueOf(target);
        }
        return target;
    }

    private double toDouble(Object val) {
        if (val == null) {
            return 0.0;
        }
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        if (val instanceof Boolean) {
            return ((Boolean) val) ? 1.0 : 0.0;
        }
        return 0.0;
    }
}
