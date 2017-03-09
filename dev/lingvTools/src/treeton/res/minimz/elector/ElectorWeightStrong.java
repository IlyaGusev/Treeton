/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.res.minimz.elector;

/**
 * ElectorWeightStrong
 * <p/>
 * Сравнивает аннотации гамматическим атрибутам.
 * Если атрибуты различаются, оставляем обе аннотации.
 * Если равны, более предпочтительной считается аннотация
 * с большим весом. Если и веса равны, то предпочитаем
 * аннотацию с максимальным id.
 */
public class ElectorWeightStrong extends ElectorWeight {
}
