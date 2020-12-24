package com.wyh.ems.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Song {
    private String song_id;
    private String song_name; //歌曲名字
    private String song_length;// 歌曲的长度
    private String genre_ids; //流派 类别
    private String artist_name; //作者
    private String musician; //作曲家
    private String zuoCijJia; //作词家
    private String language;


}

