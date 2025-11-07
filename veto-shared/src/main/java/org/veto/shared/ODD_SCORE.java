package org.veto.shared;

public enum ODD_SCORE{
        ZERO_ZERO(0, 0), // 0
        ONE_ZERO(1, 0),  // 1
        ZERO_ONE(0, 1), // 2
        ONE_ONE(1, 1), // 3
        TWO_ZERO(2, 0), // 4
        ZERO_TWO(0, 2), // 5
        TWO_ONE(2, 1), // 6
        ONE_TWO(1, 2), // 7
        TWO_TWO(2, 2), //8
        THREE_ZERO(3, 0), //9
        ZERO_THREE(0, 3), //10
        THREE_ONE(3, 1), //11
        ONE_THREE(1, 3),//12
        THREE_TWO(3, 2),//13
        TWO_THREE(2, 3),//14
        THREE_THREE(3, 3),//15
        OTHER(-1, -1);

        private final Integer mainTeam;

        private final Integer awayTeam;

        ODD_SCORE(Integer mainTeam, Integer awayTeam) {
            this.mainTeam = mainTeam;
            this.awayTeam = awayTeam;
        }

        public  String toScore(){
            return this.toScore("-");
        }

        public String toScore(String split){
            return this.mainTeam + split + this.awayTeam;
        }

        public static ODD_SCORE me(String str){
            if (str == null){
                return null;
            }

            for (ODD_SCORE value : ODD_SCORE.values()) {
                if (value.name().equalsIgnoreCase(str)){
                    return value;
                }else if ((value.mainTeam + "-" + value.awayTeam).equalsIgnoreCase(str)){
                    return value;
                }
            }

            return null;
        }

        public static ODD_SCORE me(int mainTeamScore, int awayTeamScore){
            for (ODD_SCORE value : ODD_SCORE.values()) {
                if (value.mainTeam == mainTeamScore && value.awayTeam == awayTeamScore){
                    return value;
                }
            }

            return OTHER;
        }
    }