/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.dl4j.internal;

import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.dl4j.feature.EmbeddingsFeature;
import de.tudarmstadt.ukp.dkpro.core.dl4j.feature.Feature;
import de.tudarmstadt.ukp.dkpro.core.dl4j.internal.BinaryWordVectorSerializer.BinaryVectorizer;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class Vectorize
{
    // Tagset
    private Object2IntMap<String> tagset = new Object2IntLinkedOpenHashMap<>();

    /**
     * Use for training - tagset will be built internally.
     */
    public Vectorize()
    {
        // Nothing to do
    }
    
    /**
     * Use for tagging - pre-load tagset! Mind that order of tags must be exactly as produced during
     * training.
     */
    public Vectorize(String[] aTagset)
    {
        this();
        
        for (int i = 0; i < aTagset.length; i++) {
            tagset.put(aTagset[i], i);
        }
    }
    
    public DataSet vectorize(List<List<Token>> sentences, BinaryVectorizer wordVectors,
            int truncateLength, int maxTagsetSize, boolean includeLabels)
                throws IOException
    {
        // Feature extractors
        List<Feature> featureGenerators = new ArrayList<>();
        featureGenerators.add(new EmbeddingsFeature(wordVectors));
        // featureGenerators.add(new ShapeFeature());
     
        // Get size of feature vector
        int featureVectorSize = featureGenerators.stream().mapToInt(f -> f.size()).sum();
        
        // If longest sentence exceeds 'truncateLength': only take the first 'truncateLength' words
        int maxSentLength = sentences.stream().mapToInt(tokens -> tokens.size()).max().getAsInt();
        if (maxSentLength > truncateLength) {
            maxSentLength = truncateLength;
        }

        // Create data for training
        // Here: we have sentences.size() examples of varying lengths
        INDArray features = Nd4j.create(sentences.size(), featureVectorSize, maxSentLength);
        // Tags are using a 1-hot encoding
        INDArray labels = Nd4j.create(sentences.size(), maxTagsetSize, maxSentLength);

        // Sentences have variable length, so we we need to mask positions not used in short
        // sentences.
        INDArray featuresMask = Nd4j.zeros(sentences.size(), maxSentLength);
        INDArray labelsMask = Nd4j.zeros(sentences.size(), maxSentLength);

        // Iterate over all sentences
        for (int s = 0; s < sentences.size(); s++) {
            // Get word vectors for each word in review, and put them in the training data
            List<Token> tokens = sentences.get(s);
            for (int t = 0; t < Math.min(tokens.size(), maxSentLength); t++) {
                // Look up embedding
                Token token = tokens.get(t);
                INDArray embedding = Nd4j.create(wordVectors.vectorize(token.getCoveredText()));
                features.put(new INDArrayIndex[]{ point(s), all(), point(t) }, embedding);
                
                // Word is present (not padding) -> 1.0 in features mask
                featuresMask.putScalar(new int[] { s, t }, 1.0);

                // Grow tagset if necessary
                if (!tagset.containsKey(token.getPosValue())) {
                    tagset.put(token.getPosValue(), tagset.size());
                }
                
                // Add POS label 
                labels.putScalar(s, tagset.getInt(token.getPosValue()), t, 1.0);
                labelsMask.putScalar(new int[] { s, t }, 1.0);
            }
        }

        return new DataSet(features, labels, featuresMask, labelsMask);
    }
    
    public String[] getTagset()
    {
        return tagset.keySet().toArray(new String[tagset.size()]);
    }
}
