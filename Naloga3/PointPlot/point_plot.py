#!/usr/bin/env python3
#
# Plots data given in a space separated format
#
# @author David Rubin
# Uporabljeno pri predmetu Algoritmi in analiza masivnih podatkov
import pandas
import matplotlib.pyplot as plt

def plot_file(filename):
    """
    Izrise plot nad podatki v datoteki.
    Pricakuje, da so podatki podani v obliki:
    x1 x2 ... xN y
    """
    with open(filename) as file:
        points = pandas.read_csv(filename, delimiter=' ')
    # Usually just X Y occurs, but we can also receive X1 X2 ... XN Y,
    # so check how many Xs were given
    if len(points.columns.values) <= 2:
        # We have either a polynomial or linear function, plot it so the user can decide
        points.plot(kind='scatter', x='X', y='Y')
        plt.show()
    else:
        print("The given file contains data for a multilinear function.")


if __name__ == '__main__':
    plot_file('../TestniPodatki/Linearna Funkcija/Podatki.txt')
