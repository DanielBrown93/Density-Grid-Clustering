/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package piecewisedensitygrid;

import java.util.Scanner;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;


public class PiecewiseDensityGrid {
    
    public static class GridSquare {
        public int[] dimNums; //the number of squares away from the origin square in each dimension. By default the origin square is the one directly to the positive direction of the origin in each dimension
        public int density; //the number of data points in this grid square
        public ArrayList<double[]> points; //the points in the square
        
        public GridSquare densestNeighbor;
        
        public GridSquare(int[] dn, int d, double[] p){
            this.dimNums = dn;
            this.density = d;
            this.points = new ArrayList<double[]>();
            this.points.add(p);
        }
        
        public void addPoint(double[] p){
            this.density = this.density + 1;
            points.add(p);
        }
        
        public int[] getLocation(){
            return dimNums;
        }
    }
    
    //a custom class to handle the clusters that will form
    public static class Cluster {
        public ArrayList<GridSquare> squares;
        
        public Cluster(){
            this.squares = new ArrayList<GridSquare>();
        }
        
        public void addSquare(GridSquare s){
            this.squares.add(s);
        }
        
        public boolean containsSquare(int[] squareLoc){
            for(GridSquare s : this.squares){
                if (Arrays.equals(s.dimNums, squareLoc)){
                    return true;
                }
            }
            return false;
        }
    }
    
    //need to emulate the two runs.
        //first will be the determination of grid square
            //need to set up a list of grids
                //can determine based on min/max of each 
        //second will be the neighbor detection
            //for each square
                //get list of neighbors
                //figure out which is the greatest
                //add to list
    

    
    //reads in the data from file
    public static ArrayList<double[]> readDataDouble(String fileName) {
        File file = new File(fileName);
        ArrayList<double[]> data = new ArrayList<double[]>();
        try
        {
            Scanner input = new Scanner(file);
            while (input.hasNextLine()){
                String currentLine = input.nextLine();
                String[] currentLineArray = currentLine.split(" ");
                double[] dataFromLine = new double[currentLineArray.length-1];
                for (int i=0; i<currentLineArray.length-1; i++){
                    dataFromLine[i] = Double.parseDouble(currentLineArray[i]);
                }
                data.add(dataFromLine);
            }
        }  
        catch (Exception e){
            System.out.println(e);
            return null;
        }
        return data;
    }
    
    //in data
    //out arraylist of double[]'s
    public static ArrayList<double[]> createAdaptiveGrid(ArrayList<double[]> data, int numBins, double mergePercentage){
        
        //reorder data by dimension
        double[][] axisData = new double[data.get(0).length][data.size()];
        for(int i=0; i<data.size(); i++){
            double[] tempData = data.get(i);
            for(int j=0; j<tempData.length; j++){
                axisData[j][i] = tempData[j];
            }
        }
        
        
        ArrayList<double[]> dimensionGrids = new ArrayList<double[]>();
        
        //do each axis now
        for(double[] d : axisData){
            Arrays.sort(d);
            //get data about axis and create bins
            double min = d[0]; double max = d[d.length-1];
            double range = max-min;
            double binSize = range/numBins;
            int[] bins = new int[numBins];
            
            //go through each value and assign it to a bin
            for (double d1 : d) {
              int binNum = (int) ((d1 - min) / binSize);
              if (binNum < numBins) bins[binNum] += 1;
              else bins[numBins-1] += 1;
            }
            
            //each grid is represented by a single double value, which is its starting place
            ArrayList<Double> axisGrids = new ArrayList<Double>();
            int totalDensity = bins[0]; //used for accurate average density calc
            int binsInCurrentGrid = 1;  //used for accurate average density calc
            axisGrids.add(min);
            for(int i=1; i<numBins; i++){
                double averageDensityOfCurrentGrid = totalDensity/binsInCurrentGrid;
                double dif = Math.abs(bins[i]-averageDensityOfCurrentGrid);
                //if next bin is a similar density to the first one (5%), combine them
                if (dif <= averageDensityOfCurrentGrid*mergePercentage){
                    totalDensity += bins[i]; binsInCurrentGrid++;
                }
                else {
                    axisGrids.add(min+(binSize*i));   //add a new starting place that corresponds to the current bin (aka put it into a new grid)
                    totalDensity = bins[i]; binsInCurrentGrid = 1;   //reset counter variables
                }
            }
            
            //convert arrayList<Double> to double[] so final result is an arrayList of double[]'s
            double[] grid = new double[axisGrids.size()];
            for(int i=0; i<axisGrids.size(); i++){
                grid[i] = axisGrids.get(i);
            }
            
            dimensionGrids.add(grid);
            
//            for(double d2 : grid){
//                System.out.print(d2 + ",");
//            }
//            System.out.println();
        }
        
        return dimensionGrids;
    }
    
    
    public static int findSquare(ArrayList<GridSquare> data, int[] squareLoc){
        int i = 0;
        for(GridSquare s : data){
            if (Arrays.equals(s.dimNums, squareLoc)){
                return i;
            }
            i++;
        }
        return -1;
    }
    
    public static ArrayList<GridSquare> determineGridSquaresDouble(ArrayList<double[]> data, ArrayList<double[]> adaptiveGrids){
        
        ArrayList<GridSquare> squares = new ArrayList<GridSquare>();
        
        for (double[] d : data){
            int[] squareLoc = new int[d.length];
            for (int i=0; i<d.length; i++){
                
                squareLoc[i] = determineAxisGridLoc(d[i], adaptiveGrids.get(i));
                
                
            }
            int squareIndex = findSquare(squares, squareLoc);
            if (squareIndex == -1){
                squares.add(new GridSquare(squareLoc, 1, d));
            }
            else {
                GridSquare thisSquare = squares.get(squareIndex);
                thisSquare.addPoint(d);
                squares.set(squareIndex, thisSquare);
            }
        }
        
        return squares;
    }
    
    //returns -1 if there is an error
    public static int determineAxisGridLoc(double val, double[] adaptiveGrid){
        for(int i=adaptiveGrid.length-1; i>=0; i--){
            if (val >= adaptiveGrid[i]) return i;
        }
        return -1;
    }
    
    //lets emulate the map even further
    //just have the thing make a stack, and push a grid and its neighbors each time
    
    
    
    
    
    
    
    public static boolean isNeighbor(int[] s1, int[] s2){
        for (int i=0; i<s1.length; i++){
            if (Math.abs(s2[i] - s1[i]) > 1) {return false;}
        }
        return true;
    }
    
    public static GridSquare determineDensestNeighbor2(ArrayList<GridSquare> squares, GridSquare s){
        ArrayList<GridSquare> tempSquares = (ArrayList<GridSquare>)squares.clone();
        tempSquares.remove(s);
        for (GridSquare g : squares){
            if (g.density < s.density){ break;} //only look at ones that are denser or the same
            //if (euclidianDistance(g.dimNums, s.dimNums) <= 1){ return g;}
            if (isNeighbor(g.dimNums, s.dimNums)) { return g;}
        }
        return s;
    }
    
    public static ArrayList<GridSquare> determineDensestNeighbors(ArrayList<GridSquare> squares){
        Collections.sort(squares, new Comparator<GridSquare>() { //sort the squares by density
			@Override
                        public int compare(GridSquare g1, GridSquare g2) {
                            return Integer.compare(g2.density, g1.density); //by making the comparison work backwords here, I should be able to cause the sorting to happen in descending order, without any additional steps
			}
        });
        
        for (GridSquare s : squares){
            s.densestNeighbor = determineDensestNeighbor2(squares, s);
        }
        
        return squares;
    }
    
    
    public static int findCluster(ArrayList<Cluster> clusters, int[] square){
        int i = 0;
        for(Cluster c : clusters){
            if (c.containsSquare(square)){
                return i;
            }
            i++;
        }
        return -1;
    }
    
    public static ArrayList<Cluster> determineClusters(ArrayList<GridSquare> squares){
        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
        squares = determineDensestNeighbors(squares);
        for(GridSquare s : squares){
            GridSquare sDensestNeighbor = s.densestNeighbor;
            int sClusterIndex = findCluster(clusters, s.dimNums);
            int sDensestNeighborClusterIndex = findCluster(clusters, sDensestNeighbor.dimNums);
            
            if (!s.getLocation().equals(sDensestNeighbor.getLocation())) { //if x is not its own densest neighbor
                //the cases:
                    //neither is in a cluster
                        //add both to a new cluster
                    //s is in a cluster
                        //add sdensestneighbor to the cluster
                    //sdensestneighbor is in a cluster
                        //add s to the cluster
                    //both are in a cluster
                        //merge the clusters together
                
                if (sClusterIndex == -1 && sDensestNeighborClusterIndex == -1){ //neither
                    Cluster newCluster = new Cluster();
                    newCluster.addSquare(s);
                    newCluster.addSquare(sDensestNeighbor);
                    clusters.add(newCluster);
                }
                else if (sClusterIndex != -1 && sDensestNeighborClusterIndex == -1){ //s
                    Cluster sCluster = clusters.get(sClusterIndex);
                    sCluster.addSquare(sDensestNeighbor);
                    clusters.set(sClusterIndex, sCluster);
                }
                else if (sClusterIndex == -1 && sDensestNeighborClusterIndex != -1){ //sdensestneighbor
                    Cluster sDensestNeighborCluster = clusters.get(sDensestNeighborClusterIndex);
                    sDensestNeighborCluster.addSquare(s);
                    clusters.set(sDensestNeighborClusterIndex, sDensestNeighborCluster);
                }
                else{ //both
                    Cluster sCluster = clusters.get(sClusterIndex);
                    Cluster sDensestNeighborCluster = clusters.get(sDensestNeighborClusterIndex);
                    sDensestNeighborCluster.squares.addAll(sCluster.squares);
                    clusters.set(sDensestNeighborClusterIndex, sDensestNeighborCluster);
                    clusters.remove(sClusterIndex);
                }
            }
            else{ //s is its own densest neighbor
                if (sClusterIndex == -1){
                    Cluster newCluster = new Cluster();
                    newCluster.addSquare(s);
                    clusters.add(newCluster);
                }
            }
            
            
        }
        return clusters;
    }
    
    public static void writeClusters(ArrayList<Cluster> clusters, String outputFileName){
        try
        {
            PrintWriter writer = new PrintWriter(new FileWriter(outputFileName, false));
            int i = 0;
            for (Cluster c : clusters){
                for (GridSquare s : c.squares){
                    for (double[] d : s.points){
                        for(double dim : d){
                            writer.print(dim + " ");
                        }
                        writer.println(i);
                    }
                }
                i++;
            }
            writer.close();
        }
        catch (Exception e){
            System.out.println(e);
        }
    }
    
    public static ArrayList<Cluster> run(String inputFileName, String outputFileName, int numBins, double mergePercentage){
        long startTime = System.nanoTime();
        ArrayList<double[]> data = readDataDouble(inputFileName);
        long endTime = System.nanoTime();
        //System.out.println((endTime-startTime));
        startTime = System.nanoTime();
        ArrayList<double[]> adaptiveGrids = createAdaptiveGrid(data, numBins, mergePercentage);
        endTime = System.nanoTime();
        //System.out.println((endTime-startTime));
        startTime = System.nanoTime();
        ArrayList<GridSquare> squares = determineGridSquaresDouble(data, adaptiveGrids);
        endTime = System.nanoTime();
        //System.out.println((endTime-startTime));
        startTime = System.nanoTime();
        ArrayList<Cluster> clusters = determineClusters(squares);
        endTime = System.nanoTime();
        //System.out.println((endTime-startTime));
        return clusters;
    }
    
    public static void main(String[] args) {
        
        String inputFileName = "./data2/parkinsons.txt";
        String outputFileName = "experimentalData.txt";
        
//        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
//        double highestAcc = 0;
//        double highestAccSize = 0;
//        int numBins = 100;
//        for (double i=.000; i<1; i=i+.005){
//            clusters = run(inputFileName, outputFileName, numBins, i);
//            writeClusters(clusters, outputFileName);
//            double test = AccuracyComparator.compareAccuracy(inputFileName, outputFileName);
//            if (test>=highestAcc){
//                highestAcc=test;
//                highestAccSize=i;
//            }
//            System.out.println(test + "     " + i);
//        }
//        System.out.println(highestAcc);
//        System.out.println(highestAccSize);
//        System.out.println();
        
        inputFileName = "./data2/parkinsons.txt";
        int numBins = 100;
        double mergePercentage = .33;
        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
        for(int i=0; i<10; i++){
            long startTime = System.nanoTime();
            clusters = run(inputFileName, outputFileName, numBins, mergePercentage);
            long endTime = System.nanoTime();
            System.out.println((endTime-startTime));
        }
       
        writeClusters(clusters, outputFileName);
        double test = AccuracyComparator.compareAccuracy(inputFileName, outputFileName);
        System.out.println(test);
        
    }
    
}
