package com.glacier.survivalgames.domain.entity

enum class GameState {
    Lobby,
    PreGame,
    LiveGame,
    PreDeathmatch,
    Deathmatch,

    PreFFADeathmatch,
    FFADeathmatch,

    PreTournamentDeathmatch,
    TournamentDeathmatch,

    EndGame,
    Cleanup
}