(ns guadalete-ui.util.dickens)

(def malename
  ["Morris" "Elmer" "Zechariah" "Hubert" "Tolliver"
   "Septimus" "Moulder" "Mayor" "Nathaniel" "Tom" "Marcus"
   "Hank" "Octavius" "Thorpe" "Alfie" "Simon" "Hezekiah"
   "Ralph" "Royal" "Sir" "Lord" "Dr." "Reverend" "Ellis"
   "Richard" "Dick" "Ramsey" "Rumsford" "Joss" "Alaric"
   "Renny" "Tip" "Hodge" "Floppy" "Theodore" "Ambrosius"
   "Samuel" "Treacle" "Tripp" "Hepsibah" "Jonas" "Jo" "John"
   "Lodge" "Edgar" "Rufus" "Athanias" "Latch" "Orrin" "Robert"
   "Charlie" "Badger" "James" "John St." "Will" "Watkin" "Wat"
   "Walter" "Fitzjohn" "Gib" "Gibby" "Dobby" "Nobby" "Humphrey"
   "Humbert" "Allan" "Isidore" "Harry" "Jem" "Rip" "North" "Luke"
   "Constant" "Clement" "Amory" "Moore" "Adolphus" "Arthur" "Clarence"
   "Bartholemew" "Balthazar" "Eleazar" "Ephraim" "Bentley" "Ernest"
   "Frederick" "Herbert" "Jasper" "Jarvis" "Josias" "Tommy" "Benjamin"
   "Amias" "Nat" "Ely" "Elias" "Tobias" "Marius" "Jack" "Beauregarde"
   "Wesley" "Edwin" "Horace" "Garish" "Barron" "Parrish" "Maximus"
   "Tully" "Virgilius" "Toby" "Tolly" "Victor" "Gilbert" "Bertram"
   "Hiram" "Kit" "Theodoric" "Henry" "Silas" "Cyrus" "Cyril" "Doddy"
   "Hod" "Nod" "Nudge"]
  )

(def femalename
  ["Esmeralda" "Beatrice" "Hattie" "Aggie" "Mercenaria" "Cornucopia"
   "Tetty" "Tolly" "Thistley" "Cecily" "Lucy" "Lucinda" "Polly"
   "Agnes" "Kate" "Hortense" "Agatha" "Tabitha" "Jemima" "Hermione"
   "Tatty" "Mollie" "Letty" "Maggie" "May" "Mary" "Olivetta" "Perfidia"
   "Alice" "Mrs." "Miss" "Lady" "Rose" "Adeline" "Addie" "Tilly" "Emily"
   "Amelia" "Anastasia" "Fanny" "Lucretia" "Emmy" "Ninette" "Eugenia"
   "Eudora" "Theodocia" "Aurelia" "Annie" "Tessie" "Totty" "Liza" "Lizzy"
   "Betsy" "Bess" "Netty" "Aphrodite" "Helena" "Eveny" "Etty" "Ellie" "Maisie"
   "Little" "Nonny" "Dora" "Dotty" "Theodora" "Tibby" "Sairy" "Jenny" "Herminia"
   "Constancy" "Amity" "Vera" "Myra" "Flossie" "Flora" "Rilly" "Sally" "Sadie"
   "Clara" "Annabel" "Arabella" "Clarissa" "Clarinda" "Clary" "Cordelia"
   "Dolly" "Isabelle" "Lavinia" "Ermengarde" "Lillie" "Ernestine" "Milly"
   "Jilly" "Nelly" "Marian" "Elsie" "Rosie" "Violetta" "Violet" "Sophronia"
   "Estie" "Garnet" "Ermina" "Cora" "Nora" "Eldora" "Eudosia" "Lottie" "Clella"
   "Priscilla" "Penelope" "Mamie" "Effie" "Essie" "Euphemia" "Philomena" "Philomela"
   "Florrie" "Kitty" "Maude" "Minnie" "Mimsy" "Tabby" "Tibby" "Victoria" "India"
   "Prunella" "Prudence"])

(def suffix
  [" " "sby" "s" "by" "ingham" "ham" "witch" "wick" "er" "ner" "field"
   "nard" "ingford" "lethwaite" "waite" "white" "ette" "ers" "comb"
   "ners" "ingwhite" "thorpe" "enthorpe" "bourne" "ingborne" "lock"
   "el" "le" "ty" "inner" "ing" "ings" "in" "ins" "ington" "ocks" "um"
   "ton" "ston" "stone" "ingstone" "ery" "nery" "ets" "etts" "ock"
   "et" "son" "on" "ingson" "enson" "let" "lette" "net" "ret" "cock"
   "ly" "ley" "leigh" "erly" "erby" "elby" "les" "less" "it" "itt"
   "elet" "elay" "lier" "nay" "enay" "erny" "eny" "kin" "kins" "ikin"
   "ingly" "mer" "imer" "mere" "ymere" "y" "ily" "illy" "ity" "lety"
   "fuss" "lington" "game" "ingame" "ymede" "itty" "ful" "ybert" "pole"
   "sy" "sly" "sty" "yham" "ry" "ery" "ingly" "with" "thwaite" "ler"
   "ier" "borough" "row" "rot" "let" "ybone" "lop" "ick" "ickle" "elsby"
   "ling" "lind" "age" "itage" "ersly" "ersby" "full" "ton" "itch" "ett"
   "idge" "ish" "nock" "rich" "sible" "able" "stable" "ny" "nar" "net"
   "ridge" "eridge" "erage" "lethorpe" "lefield" "ingfield" "ykins" "ers"
   "ford" "ness" "nell" "iness" "ress" "ster" "sbridge" "man" "wright"
   "is" "iss" "us" "land" "smith"]
  )

(def lastname
  ["Chalk" "Caulk" "Cramp" "Crump" "Crimp" "Scrimp" "Scriv" "Bunk"
   "Felch" "Fist" "Botch" "Butt" "Rum" "Drum" "Ratch" "Bor" "Funk" "Boot"
   "Cox" "Spang" "Clatch" "Glitch" "Dizz" "Dith" "Pith" "Posh" "Kil"
   "Tar" "Gros" "Pos" "Hang" "Mal" "Mick" "Todd" "Toad" "Read" "Wretch"
   "Crotch" "Torch" "Bigg" "Begg" "Bagg" "Snarf" "Scarf" "Scar" "Scarm"
   "Mar" "Scam" "Scamp" "Bung" "Bun" "Dash" "Crash" "Cask" "Run" "Nash"
   "Crow" "Flan" "Flint" "Pank" "Rank" "Span" "Swin" "Swigg" "Arf" "Trol"
   "Bark" "March" "Lark" "Lurk" "Fur" "Bar" "Rat" "Cat" "Dogg" "Tripp"
   "Master" "Eat" "East" "Beat" "Bet" "Meat" "Need" "Budd" "Catch" "Tudd"
   "Striv" "Spiv" "Sniv" "Knav" "Tugg" "Mump" "Frump" "Egg" "Rump" "Dith"
   "Tibb" "Tubb" "Slopp" "Mopp" "Grop" "Dropp" "Wreck" "Gum" "Pain" "Ash"
   "Rack" "Arm" "Hand" "Legg" "Head" "Beet" "Beed" "Beer" "Wren" "Ros"
   "Twitch" "Peer" "Fatt" "Lack" "Thin" "Bedd" "Tramp" "Stalk" "Rid"
   "Skin" "Raw" "Quin" "Quil" "Squil" "Squeal" "Squal" "Quash" "Quid"
   "Bugg" "Digg" "Pigg" "Bump" "Popp" "Snark" "Spit" "Pitch" "Sharp"
   "Slym" "Beck" "Char" "Codd" "Trout" "Slough" "Feather" "Curd" "Shil"
   "Chil" "Fudd" "Blather" "Bath" "Bust" "Bunt" "Brunt" "Black" "Green"
   "Plumm" "Bumm"]
  )

(defn- make! [first-name]
       (let [
             last-name* (rand-nth lastname)
             first-name* (rand-nth first-name)
             suffix* (rand-nth suffix)]
            (str first-name* " " last-name* suffix*)))

(defn generate-name
      "Generates a random 'dickensiesque' name"
      []
      (if (< 0.5 (rand))
        (make! femalename)
        (make! malename)))