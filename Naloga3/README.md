# Linearna regresija

Pri nalogi morate implementirati linearno regresijo, kjer iz podatkov pridobite koeficiente funkcije.

Vaša aplikacija mora biti sestavljena iz treh delov. Prvi del je predobdelava podatkov, drugi del je linearna regresija in tretji del je napovedovanje vrednosti ciljne spremenljivke na podlagi najdene funkcije.

V podatkih imamo zapisane razlagalne spremenljivke (X) ter ciljno spremenljivko, ki jo napovedujemo (Y). Vhodni podatki so organizirani na sledeč način:
```
X Y
1 3.1
4 4.7
7 6
2 3.4
6 5.3
```
Vhodni podatki nam lahko predstavljajo linearno, polinomsko ali pa funkcijo z več neodvisnimi spremenljivkami.

V prvem koraku morate vhodne podatke predobdelati, kjer morate ugotoviti, (približno) kakšna funkcija se skriva v podatkih. To ugotovite najlažje tako, da vrednosti izrišete. V primeru vhodnih podakov, kjer imate več vrednosti X, imate funkcijo z več neodvisnimi spremenljivkami, v nasprotnem primeru pa morate sami ugotoviti kakšna oblika funkcije se skriva v podatkih (linearna ali polinomska).

Vhodni podatki funkcije z več neodvisnimi spremenljivkami:
```
X1 X2 X3 X4 Y
1 7 -2 1 311
```
Ko se odločite za obliko funkcije, morate vhodne podatke prilagoditi linearni regresiji, kar pomeni, da morate izgraditi pravilno matriko X.

V naslednjem koraku pričnete z linearno regresijo nad matriko X in Y. Glej priložen dokument s postopkom.

Pri implementaciji linearne regresije (metode najmanjših kvadratov) morate vse funkcije za delo z matrikami implementirati sami! Samo za računanje inverzne matrike lahko uporabite knjižnico.

