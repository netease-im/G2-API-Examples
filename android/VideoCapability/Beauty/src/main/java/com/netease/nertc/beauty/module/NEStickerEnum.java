package com.netease.nertc.beauty.module;

import com.netease.nertc.beauty.R;

import java.util.HashMap;

public enum NEStickerEnum {
    ORIGIN(R.id.rb_sticker_origin, "origin"),
    EAT(R.id.rb_sticker_eatZongzi, "eatZongzi"),
    DRINK(R.id.rb_sticker_drinkBeer, "drinkBeer"),
    DRINK2(R.id.rb_sticker_drinkBeer2, "drinkBeer2"),
    GRASS(R.id.rb_sticker_yes, "grass"),
    KISS(R.id.rb_sticker_kiss, "kiss1"),
    KISS3(R.id.rb_sticker_kiss3, "kiss3"),
    KISS4(R.id.rb_sticker_kiss4, "kiss4"),
    MONEY_RAIN(R.id.rb_sticker_money_rain, "moneyRain"),
    GIFT(R.id.rb_sticker_gift, "gift"),
    FLOWER(R.id.rb_sticker_flower, "flower"),
    MOUTH(R.id.rb_sticker_lipstick, "lipstick"),
    KISS2(R.id.rb_sticker_kiss2, "kiss2"),
    BUNNY(R.id.rb_sticker_bunny, "bunny"),
    HEART(R.id.rb_sticker_heart, "heart"),
    HEART2(R.id.rb_sticker_heart2, "heart2"),
    HEART3(R.id.rb_sticker_heart3, "heart3"),
    CROWN(R.id.rb_sticker_crown, "crown"),
    PACKAGE(R.id.rb_sticker_package, "package"),
    RABBIT_EATING(R.id.rb_sticker_rabbiteating, "rabbiteating"),
    GLASS(R.id.rb_sticker_glass, "glass");

    private int resId;
    private String name;

    NEStickerEnum(int resId, String name) {
        this.resId = resId;
        this.name = name;
    }

    public static HashMap<Integer, NESticker> getStickers() {
        NEStickerEnum[] stickerEnums = NEStickerEnum.values();
        HashMap<Integer, NESticker> stickers = new HashMap<>();
        for (NEStickerEnum sticker : stickerEnums) {
            stickers.put(sticker.resId, new NESticker(sticker.resId, sticker.name));
        }
        return stickers;
    }
}
